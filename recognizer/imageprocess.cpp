#include "types.h"
#include "utils.h"
#include "vimage.h"
#include "sat.h"

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
	if (!similar(arr[0], arr[1])) return false;
	if (!similar(arr[2], arr[3])) return false;
	if (arr[2] < n/4 || arr[3] < n/4) return false;
	if (min(longestRuns[0][1], longestRuns[1][1]) < max(longestRuns[0][0], longestRuns[1][0]) / 2) return false;
	return true;
}

vector<Pt> findMarkers(VImage& image)
{
	const int radii[] = { 8, 12, 15, 16, 18, 20 };
	const int NR = COUNT_OF(radii);
	const int maxRadius = radii[NR - 1];
	genOffsets(radii, NR);
	int w = image.w;
	int h = image.h;
	
	SAT sat(w, h);
	FOR(y, h) FOR(x, w) sat.setPixel(x, y, !!image.getpixel(x, y).r);
	sat.prepareSAT();
	
	printf("."); fflush(stdout);
	typedef pair<Pt, int> Cand;
	vector<Cand> cands; 
	for (int y = maxRadius + 1; y < h - maxRadius - 1; y++)
		for (int x = maxRadius + 1; x < w - maxRadius - 1; x++) {
			if (sat.pureColorAround(x, y, 5)) continue;
//			if (x == 1083 && y == 600)
			if (isMarker(image, x, y, 0)) {
				int sz = 1;
				while (sz < NR) {
					if (!isMarker(image, x, y, sz)) break;
					sz++;
				}
				if (sz >= NR)
					cands.push_back(make_pair(Pt(x, y), sz));
			}
		}
	printf("."); fflush(stdout);
	stable_sort(cands.begin(), cands.end(), [](const Cand& left, const Cand& right) { return left.second > right.second; });
	printf("."); fflush(stdout);
	//
//	printf("Markers: \n");
//	REP(i, cands) printf("score %d, (%d, %d)\n", cands[i].second, (int) cands[i].first.x, (int) cands[i].first.y);
//	REP(i, cands) 
//		image.putpixel(cands[i].first.x, cands[i].first.y, RGBA(0xff, 0, 0));
	vector<vector<Pt> > clusters;
	const int CLUSTER_RANGE = 20;
	REP(i, cands) {
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
