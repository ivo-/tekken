#ifndef __SAT_H__
#define __SAT_H__

class SAT {
	int* data;
	int w, h;
	inline int get(int x, int y) { return data[y * w  + x]; }

public:
	SAT(int width, int height);
	void setPixel(int x, int y, int value);
	void prepareSAT();
	int query(int x1, int y1, int x2, int y2);
	
	bool pureColorAround(int x, int y, int radius);
};

#endif // __SAT_H__
