#include "types.h"
#include "utils.h"
#include "vimage.h"
#include "sat.h"
#include "imageprocess.h"
#include "main.h"

vector<vector<Pt> > offsets;

static void genOffsets(const int radii[], int nr)
{
	for (int k = 0; k < nr; k++) {
		vector<Pt> off;
		int R = radii[k];
		int numSteps = (int) ceil(R * 6.28);
		for (int i = 0; i < numSteps; i++) {
			double angle = i / double(numSteps) * 2 * M_PI;
			off.push_back(Pt((int) round(cos(angle) * R), (int) round(sin(angle) * R)));
		}
		offsets.push_back(off);
	}
}

static void updateRuns(int longestRuns[], int runLength)
{
	if (runLength > longestRuns[0]) {
		longestRuns[1] = longestRuns[0];
		longestRuns[0] = runLength;
	} else if (runLength > longestRuns[1])
		longestRuns[1] = runLength;
}


// a and b should be within 15% of each other, +-1
static bool similar(int a, int b)
{
	if (a > b) swap(a, b);
	int c = ceil(a * 1.15 + 1);
	return b <= c;
}

static bool isMarker(const VImage& image, int x, int y, int rindx)
{
	const vector<Pt>& off = offsets[rindx];
	//
	int n = off.size();
	bool stats[n];
	REP(i, off)
		stats[i] = !!image.getpixel(x + off[i].x, y + off[i].y).r;
	//
	int start = 0;
	while (start < n && stats[start] == stats[(start + 1) % n]) start++;
	if (start >= n) return false;
	start = (start + 1) % n;
	int i = start;
	int longestRuns[2][2] = { { -1, -1 }, { -1, -1 } };
	do {
		int cnt = 0;
		bool color = stats[i];
		while (stats[i] == color) {
			i = (i + 1) % n;
			cnt++;
		}
		//
		updateRuns(longestRuns[int(color)], cnt);
	} while (i != start);
	
	int threshold = 0.15 * n;
	if (longestRuns[0][1] < threshold || longestRuns[1][1] < threshold) return false;
	int arr[4] = { longestRuns[0][0], longestRuns[0][1], longestRuns[1][0], longestRuns[1][1] };
	sort(arr, arr + 4);
	int numSimilar = 0;
	FOR(i, 3) if (similar(arr[i], arr[i + 1])) numSimilar++;
	if (numSimilar == 0) return false;
	if (arr[2] < n/4 || arr[3] < n/4) return false;
//	if (min(longestRuns[0][1], longestRuns[1][1]) < max(longestRuns[0][0], longestRuns[1][0]) / 2) return false;
	return true;
}

typedef pair<Pt, int> Cand;
vector<Pt> clusterize(const vector<Cand>& cands, int minScore)
{
	vector<vector<Pt> > clusters;
	const int CLUSTER_RANGE = 20;
	REP(i, cands) {
		if (cands[i].second < minScore) continue;
		Pt t = cands[i].first;
		int foundIdx = -1;
		for (int j = (int) clusters.size() - 1; j >= 0; j--) if (dist(t, clusters[j][0]) < CLUSTER_RANGE) {
			foundIdx = j;
			break;
		}
		if (foundIdx == -1) {
			REP(j, clusters) {
				REP(k, clusters[j]) if (dist(t, clusters[j][k]) < CLUSTER_RANGE) {
					foundIdx = j;
					break;
				}
				if (foundIdx != -1) break;
			}
		}
		if (foundIdx != -1)
			clusters[foundIdx].push_back(t);
		else
			clusters.push_back(vector<Pt>(1, t));
	}
	printf("."); fflush(stdout);
	vector<Pt> results;
	REP(i, clusters) {
		Pt average(0, 0);
		REP(j, clusters[i]) average += clusters[i][j];
		average /= double(clusters[i].size());
		results.push_back(average);
	}
	printf("."); fflush(stdout);
	return results;
	
}

static bool hasWhiteFrame(const SAT& sat, int x, int y)
{
	int r = 10;
	int prev = sat.sampleBlack(x, y, r), next;
	while (r < 100 && (next = sat.sampleBlack(x, y, r * 4 / 3) != prev)) {
		r = r * 4 / 3;
		prev = sat.sampleBlack(x, y, r);
	}
	if (r >= 100) return false;
	return sat.sampleBlack(x, y, r * 2) == prev;
}

vector<Pt> findMarkers(VImage& image)
{
	const int radii[] = { 10, 12, 14, 16, 20 };
	const int NR = COUNT_OF(radii);
	const int MID = NR/2;
	const int maxRadius = radii[NR - 1];
	genOffsets(radii, NR);
	int w = image.w;
	int h = image.h;
	
	SAT sat(w, h);
	FOR(y, h) FOR(x, w) sat.setPixel(x, y, !!image.getpixel(x, y).r);
	sat.prepareSAT();
	
	printf("."); fflush(stdout);
	vector<Cand> cands; 
	for (int y = maxRadius + 1; y < h - maxRadius - 1; y++)
		for (int x = maxRadius + 1; x < w - maxRadius - 1; x++) {
			if (sat.pureColorAround(x, y, 7)) continue;
//			if (x == 766 && y == 650)
			if (isMarker(image, x, y, MID)) {
				int lo = MID - 1;
				while (lo >= 0 && isMarker(image, x, y, lo)) lo--;
				lo++;
				int hi = MID + 1;
				while (hi < NR && isMarker(image, x, y, hi)) hi++;
				int sz = hi - lo;
				if (sz >= 2 && hasWhiteFrame(sat, x, y))
					cands.push_back(make_pair(Pt(x, y), sz));
			}
		}
	printf("."); fflush(stdout);
	stable_sort(cands.begin(), cands.end(), [](const Cand& left, const Cand& right) { return left.second > right.second; });
	printf("."); fflush(stdout);
	//
	for (int minScore = NR; minScore >= NR/2; minScore--) {
		vector<Pt> clusters = clusterize(cands, minScore);
		if (clusters.size() >= 5) return clusters;
	}
	return clusterize(cands, 0);
	//
//	printf("Markers: \n");
//	REP(i, cands) printf("score %d, (%d, %d)\n", cands[i].second, (int) cands[i].first.x, (int) cands[i].first.y);
//	REP(i, cands) 
//		image.putpixel(cands[i].first.x, cands[i].first.y, RGBA(0xff, 0, 0));
}

const float dH = 34.5, dV = 30.3;
int cellsize = 8;

int grid(VImage& image, float x, float y, int entries, float dx, float dy)
{
	int stats[entries];
	FOR(i, entries) {
		stats[i] = 0;
		int cx = round(x + dx * i);
		int cy = round(y + dy * i);
		for (int py = cy - cellsize; py <= cy + cellsize; py++)
			for (int px = cx - cellsize; px <= cx + cellsize; px++) {
				stats[i] += image.getpixel(px, py).sum();
				image.mark(px, py);
			}
	}
//	printf("grid(%f, %f, %d, %f, %f) = {", x, y, entries, dx, dy);
//	FOR(i, entries) {
//		if (i) printf(", ");
//		printf("%d", stats[i]);
//	}
//	printf("}\n");
	vector<pair<float, int> > s;
	FOR(i, entries) s.push_back(make_pair(float(max(1, stats[i])), i));
	sort(s.begin(), s.end());
	
	int idx = 0;
	while (idx < entries - 1 && s[idx + 1].first / s[idx].first < 1.02f) idx++;
	if (idx > 0) return -1; 
	return s[0].second;
}

TestData recognize(VImage& image)
{
	TestData td;
	const int OFF_BITS_X[4] = { 56, 75, 93, 109 };
	const int OFF_BITS_Y = 60;
	
	
	const int sX = 23, sY = 164;
	const int spacer1 = 112, spacer2 = 64;
	
	VImage bin = image;
	bin.binarize(bin.getOtsuThreshold());
	
	
	int bits[4];
	FOR(i, 4)
		bits[i] = !bin.sample(OFF_BITS_X[i], OFF_BITS_Y, 3);
	td.variant = 0;
	FOR(i, 4)
		td.variant = td.variant * 2 + bits[i];
	
	//
	float x = sX, y = sY;
	FOR(fni, numFn) {
		td.fn[fni] = grid(image, x, y, 10, dH, 0);
		y += dV;
	}
	
	y -= dV;
	y += spacer1;
	
	FOR(qi, min(15, numQ)) {
		td.answers[qi] = grid(image, x, y, numA, 0, dV);
		x += dH;
	}
	
	x = sX;
	y += dV * (numA - 1);
	
	y += spacer2;
	
	for (int qi = 15; qi < numQ; qi++) {
		td.answers[qi] = grid(image, x, y, numA, 0, dV);
		x += dH;
	}

#ifdef DEBUG
	bin.save("/home/vesko/persp-bin.png");
	image.save("/home/vesko/persp-full.png");
#endif
	
	return td;
}
