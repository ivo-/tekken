#include <wx/wx.h>
#include <wx/image.h>
#include "types.h"
#include "vimage.h"
#include "perspective.h"
#include "utils.h"
#include "imageprocess.h"

//////////////// Usage of this library - sample code:

const int TWIDTH = 536;
const int THEIGHT = 700;

// sample params: 
// recognizer <source.jpg> <# questions> <# answers> <# digits in FN> <outfile.txt>


string inputFile;
string outFile;
int numQ, numA, numFn;

bool parseCmdLine(int argc, char** argv)
{
	if (argc != 6) {
		printf("Usage: recognizer <source.jpg> <# questions> <# answers> <# digits in FN> <outfile.txt>\n");
		return false;
	}
	
	inputFile = argv[1];
	if (!wxFileExists(WXSTRING(inputFile))) {
		printf("Cannot open input file `%s'\n", argv[1]);
		return false;
	}
	
	if (1 != sscanf(argv[2], "%d", &numQ) || numQ < 3 || numQ > 50) {
		printf("Number of questions has to be between 3 and 50\n");
		return false;
	}
	if (1 != sscanf(argv[3], "%d", &numA) || numA < 2 || numA > 5) {
		printf("Number of answers per question has to be between 2 and 5\n");
		return false;
	}
	if (1 != sscanf(argv[4], "%d", &numFn) || numFn < 3 || numFn > 8) {
		printf("Number of FacultyNumber digits has to be between 3 and 8\n");
		return false;
	}
	
	FILE* test = fopen(argv[5], "wt");
	if (!test) {
		printf("Cannot open the output file `%s'\n", argv[5]);
		return false;
	}
	fclose(test);
	outFile = argv[5];
	//
	return true;
}

static bool straightlined(const Pt& a, const Pt& b, const Pt& c)
{
	double d1 = dist(a, c);
	double d2 = dist(a, b);
	double d3 = dist(b, c);
	double sum = d2 + d3;
	if (d1 < sum * 0.95 || d1 > sum * 1.05) return false;
	if (min(d2, d3) < 0.8 * max(d2, d3)) return false;
	double angle = getAngle(a, b, c);
	return angle > 175;
}

void filterMarkers(vector<Pt>& markers)
{
	double maxArea = -1;
	int indices[5];
	
	int n = (int) markers.size();
	FOR(i, n)
	FOR(j, n) if (j != i)
	FOR(k, n) if (k != i && k != j)
	if (straightlined(markers[i], markers[j], markers[k])) {
		FOR(l, n) if (l != k && l != j && l != i)
		FOR(t, n) if (t != l && t != k && t != j && t != i) {
			double S1 = area(markers[i], markers[l], markers[t]);
			if (S1 < 0) continue;
			if (area(markers[l], markers[t], markers[k]) < 0) continue;
			double S2 = area(markers[t], markers[k], markers[i]);
			if (S2 < 0) continue;
			if (area(markers[k], markers[i], markers[l]) < 0) continue;
			double S = S1 + S2;
			if (S > maxArea) {
				maxArea = S;
				indices[0] = i;
				indices[1] = j;
				indices[2] = k;
				indices[3] = l;
				indices[4] = t;
			}
		}
	}
	//
	if (maxArea < 0) {
		markers.clear();
		return;
	}
	vector<Pt> result;
	FOR(i, 5) result.push_back(markers[indices[i]]);
	markers = result;
}



int main(int argc, char** argv)
{
	if (!parseCmdLine(argc, argv)) return 1;
	wxInitAllImageHandlers();
	printf("Read\n");
	wxImage img(WXSTRING(inputFile));
	if (!img.Ok()) {
		printf("Cannot open image!\n");
		return 1;
	}
	VImage image(img);
	while (image.w * image.h > 10000000)
		image.resizeHalf();
	printf("grayscale\n");
	image.to_grayscale();
	printf("falloff\n");
	image.fix_falloff();
	printf("Otsu\n");
	int thresh = image.getOtsuThreshold();
	
	VImage colorful = image;
	image.binarize(thresh);
	
	printf("Markers..."); fflush(stdout);
	vector<Pt> markers = findMarkers(image);
	printf("%d raw markers\n", (int) markers.size());
	REP(i, markers)
		FOR(dy, 3) FOR(dx, 3)
			image.putpixel(dx - 1 + markers[i].x, dy - 1 + markers[i].y, RGBA(0xff, 0, 0));
			
	printf("FilterMarkers..."); fflush(stdout);
	filterMarkers(markers);

	printf("%d markers\n", int(markers.size()));
	REP(i, markers)
		FOR(dy, 3) FOR(dx, 3)
			image.putpixel(dx - 1 + markers[i].x, dy - 1 + markers[i].y, RGBA(0, 0xff, 0));
	REP(i, markers) printf("(%d, %d) ", (int) markers[i].x, (int) markers[i].y);
	printf("\n");
	
	printf("Transform\n");
	Pt corners[4] = { markers[0], markers[2], markers[3], markers[4] };
	Pt destpoints[4]; // where the four corners will be mapped onto (will hold vertices of some rectangle)
	destpoints[0] = Pt(0, 0);
	destpoints[1] = Pt(TWIDTH, 0);
	destpoints[2] = Pt(0, THEIGHT);
	destpoints[3] = Pt(TWIDTH, THEIGHT);
	auto transformMatrix = getPerspectiveTransform(corners, destpoints);
	auto perspectived = new VImage(transformPerspective(colorful, transformMatrix, TWIDTH, THEIGHT));
	image = *perspectived;
	image.binarize(thresh);
	
	if (image.sample(6, 6, 3) == 0) {
		perspectived->flipX();
		image.flipX();
	}
	
	printf("Save\n");
	perspectived->save(outFile);
	
	return 0;
}
