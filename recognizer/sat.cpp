
#include "types.h"
#include "sat.h"

SAT::SAT(int width, int height)
{
	w = width + 1;
	h = height + 1;
	data = new int[w * h];
	memset(data, 0, sizeof(int) * w * h);
}

void SAT::setPixel(int x, int y, int value)
{
	data[(y + 1) * w + (x + 1)] = !!value;
}

void SAT::prepareSAT()
{
	for (int y = 1; y < h; y++)
		for (int x = 1; x < w; x++)
			data[y * w + x] += data[(y - 1) * w + x] + data[y * w + x - 1] - data[(y - 1) * w + x - 1];
}


// sum of pixel values within [x1..x2] x [y1 .. y2]
int SAT::query(int x1, int y1, int x2, int y2)
{
	x1 += 1;
	y1 += 1;
	x2 += 1;
	y2 += 1;
	return get(x2, y2) - get(x1 - 1, y2) - get(x2, y1 - 1) + get(x1 - 1, y1 - 1);
}

bool SAT::pureColorAround(int x, int y, int radius)
{
	int x1 = max(0, x - radius);
	int x2 = min(w - 1, x + radius);
	int y1 = max(0, y - radius);
	int y2 = min(h - 1, y + radius);
	int area = (x2 - x1 + 1) * (y2 - y1 + 1);
	int q = query(x1, y1, x2, y2);
	return q == area || q == 0;
}

