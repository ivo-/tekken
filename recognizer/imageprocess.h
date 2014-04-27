#ifndef __IMAGEPROCESS_H__
#define __IMAGEPROCESS_H__

#include "types.h"

struct TestData {
	int variant;
	int fn[10];
	int answers[60];
};

vector<Pt> findMarkers(VImage& image);

TestData recognize(VImage& image);

#endif // __IMAGEPROCESS_H__
