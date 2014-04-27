#ifndef __VIMAGE_H__
#define __VIMAGE_H__

#include "types.h"


class wxImage;
class VImage {

	void _kopy(const VImage& rhs)
	{
		w = rhs.w;
		h = rhs.h;
		data = new RGBA[w*h];
		gain = rhs.gain;
		memcpy(data, rhs.data, w * h * sizeof(RGBA));
	}
	void _destroy() { if (data) delete[] data; data = NULL; }
	inline int clampX(int x) const
	{
		if (x < 0) x = 0;
		if (x >= w) x = w - 1;
		return x;
	}
	inline int clampY(int y) const
	{
		if (y < 0) y = 0;
		if (y >= h) y = h - 1;
		return y;
	}
	float gain;
public:
	int w, h;
	RGBA* data;

	VImage(const wxImage& img);
	VImage(int w, int h): w(w), h(h)
	{
		gain = 1.0f;
		data = new RGBA[w*h];
		memset(data, 0, w * h * sizeof(RGBA));
	}
	VImage(const VImage& rhs)
	{
		_kopy(rhs);
	}
	VImage& operator = (const VImage& rhs)
	{
		if (&rhs != this) {
			_destroy();
			_kopy(rhs);
		}
		return *this;
	}
	~VImage() { _destroy(); }
	VImage(const std::vector<int>&);

	inline RGBA getpixel(int x, int y) const
	{
		if (unsigned(x) >= unsigned(w) || unsigned(y) >= unsigned(h))
			return RGBA(0);
		return data[y * w + x];
	}
	inline void putpixel(int x, int y, const RGBA& pixel)
	{
		if (!(unsigned(x) >= unsigned(w) || unsigned(y) >= unsigned(h)))
			data[y * w + x] = pixel;
	}
	void clampPos(int& x, int& y)
	{
		if (x < 0) x = 0;
		if (x >= w) x = w - 1;
		if (y < 0) y = 0;
		if (y >= h) y = h - 1;
	}
	float getGain() const { return gain; }
	void normalize_brightness(int target_midlevel = 128);
	void normalize_brightness(Pt center, int radius, int target_midlevel = 128);
	void to_grayscale(void);
	void gaussian_blur(int radius);
	void sobel(void); // assumes the image has been grayscaled first
	int getOtsuThreshold(void) const; // assumes the image has been grayscaled first
	void binarize(int threshold); // assumes the image has been grayscaled first
	void multiply(float f);
	void multiply(float fr, float fg, float fb);
	void white_balance(int x1, int y1, int x2, int y2); // apply white balance, sampling from the selected rectangle
	int num_different_colors(void) const; // total number of differnt colors, up to 2^24
	void fix_falloff(void);
	void enlarge(float factor);
	void resizeHalf(void);
	
	int sample(int x, int y, int size); // check if the square with length `size', centered at `x', `y' is mostly white or black
	void flipX(); // flip image by X
	
	void save(const string& fn);
	void mark(int x, int y);
};

#endif // __VIMAGE_H__
