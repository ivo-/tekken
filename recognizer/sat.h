#ifndef __SAT_H__
#define __SAT_H__

class SAT {
	int* data;
	int w, h;
	inline int get(int x, int y) const { return data[y * w  + x]; }

public:
	SAT(int width, int height);
	void setPixel(int x, int y, int value);
	void prepareSAT();
	int query(int x1, int y1, int x2, int y2) const;
	
	bool pureColorAround(int x, int y, int radius) const;
	
	// returns the number of BLACK pixels in a square (2R + 1) x (2R + 1) large, centered around x,y
	int sampleBlack(int x, int y, int R) const;
};

#endif // __SAT_H__
