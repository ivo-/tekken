#include <wx/wx.h>
#include <wx/image.h>
#include "types.h"
#include "vimage.h"
#include "perspective.h"
#include "utils.h"

//////////////// Usage of this library - sample code:

const int TWIDTH = 800;
const int THEIGHT = 400;

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

int main(int argc, char** argv)
{
	if (!parseCmdLine(argc, argv)) return 1;
	wxInitAllImageHandlers();
	wxImage img(WXSTRING(inputFile));
	if (!img.Ok()) {
		printf("Cannot open image!\n");
		return 1;
	}
	VImage image(img);
	image.to_grayscale();
	image.fix_falloff();
	int thresh = image.getOtsuThreshold();
	
	VImage colorful = image;
	image.binarize(thresh);
	
	image.save(outFile);
	/*
	Pt corners[4]; // the four detected corners on the scanned image (holds vertices to an arbitrary quadrilateral)
	Pt destpoints[4]; // where the four corners will be mapped onto (will hold vertices of some rectangle)
	destpoints[0] = Pt(0, 0);
	destpoints[1] = Pt(TWIDTH, 0);
	destpoints[2] = Pt(0, THEIGHT);
	destpoints[3] = Pt(TWIDTH, THEIGHT);
	auto transformMatrix = getPerspectiveTransform(corners, destpoints);
	auto perspectived = new VImage(transformPerspective(image, transformMatrix, TWIDTH, THEIGHT));
	*/
	return 0;
}
