
#include "types.h"
#include "vimage.h"
#include "utils.h"
#include <wx/image.h>

void VImage::fix_falloff(void)
{
	const int N = 24;
	double intensity[N][N] = {};
	int alle[N][N] = {};
	FOR(y, h) FOR(x, w) {
		int dy = y * N / h;
		int dx = x * N / w;
		alle[dy][dx]++;
		intensity[dy][dx] += data[y * w + x].sum();
	}
	float C[N][N];
	FOR(i, N) FOR(j, N) {
		double correction = 128 / (intensity[i][j] / (alle[i][j] * 3));
		if (correction < 0.5) correction = 0.5;
		if (correction > 2.0) correction = 2.0;
		C[i][j] = correction;
	}
	float avgGain = 0;
	FOR(i, N) FOR(j, N) avgGain += C[i][j];
	gain *= (avgGain / (N*N));
	FOR(y, h) FOR(x, w) {
		float fx = x * N / float(w) - 0.5;
		float fy = y * N / float(h) - 0.5;
		int ix = (int) floor(fx);
		int iy = (int) floor(fy);
		float f00 = C[max(    0, iy    )][max(    0, ix    )];
		float f01 = C[max(    0, iy    )][min(N - 1, ix + 1)];
		float f10 = C[min(N - 1, iy + 1)][max(    0, ix    )];
		float f11 = C[min(N - 1, iy + 1)][min(N - 1, ix + 1)];
		float px = fx - ix, py = fy - iy;
		float coeff = f00 * (1 - px) * (1 - py) + f01 * px * (1 - py) + f10 * (1 - px) * py + f11 * px * py;
		int i = (y * w + x);
		data[i].r = (uint8) min(255, (int) floor(0.5f + data[i].r * coeff));
		data[i].g = (uint8) min(255, (int) floor(0.5f + data[i].g * coeff));
		data[i].b = (uint8) min(255, (int) floor(0.5f + data[i].b * coeff));
	}
}

int VImage::getOtsuThreshold(void) const
{
	int hist[256] = {0};

	int total = 0;
	const float min_range = 0.2, max_range = 0.8;
	for (int y = h * min_range; y < h * max_range; y++)
		for (int x = w * min_range; x < w * max_range; x++) {
			total++;
			hist[data[y * w + x].r]++;
		}
	float sum = 0.0;
	for (int i = 0; i < 256; i++)
		sum += i * (float) hist[i];
	float sumB = 0;
	int wB = 0, wF = 0;
	float maxVar = 0;
	int bestThreshold = 0;
	for (int t = 0; t < 256; t++) {
		wB += hist[t];
		if (wB == 0) continue;
		wF = total - wB;
		if (wF == 0) break;
		sumB += t * (float) hist[t];
		float mB = sumB / (float) wB;
		float mF = (sum - sumB) / (float) wF;
		float varBetween = (float) wB * (float) wF * sqr(mB - mF);
		if (varBetween > maxVar) {
			maxVar = varBetween;
			bestThreshold = t;
		}
	}
	return bestThreshold;
}

void VImage::binarize(int threshold)
{
	// otsu binarization:
	FOR(i, w*h) {
		if (data[i].r > threshold) data[i] = RGBA(0x5f, 0x5f, 0x5f, data[i].a);
		else data[i] = RGBA(0, 0, 0, data[i].a);
	}
}

VImage::VImage(const wxImage& img)
{
	construct_empty(img.GetWidth(), img.GetHeight());
	FOR(y, h) FOR(x, w) {
		data[y * w + x] = RGBA(img.GetRed(x, y), img.GetGreen(x, y), img.GetBlue(x, y));
	}
}

void VImage::save(const string& fn)
{
	wxImage img(w, h);
	int ii = 0;
	FOR(y, h) FOR(x, w) {
		img.SetRGB(x, y, data[ii].r, data[ii].g, data[ii].b);
		ii++;
	}
	img.SaveFile(WXSTRING(fn));
}

void VImage::to_grayscale(void)
{
	for (int i = 0; i < w*h; i++) {
		uint8 mid = (uint8) (((unsigned) data[i].r + (unsigned) data[i].g + (unsigned) data[i].b) / 3);
		data[i].r = data[i].g = data[i].b = mid;
	}
}

void VImage::resizeHalf(void)
{
	//return;
	int nw = w/2;
	int nh = h/2;
	RGBA* ndata = new RGBA[nw * nh];
	FOR(y, nh)
		FOR(x, nw) {
			int r = 4, g = 2, b = 2;
			r += data[(y * 2    )*w + x * 2    ].r;
			g += data[(y * 2    )*w + x * 2    ].g;
			b += data[(y * 2    )*w + x * 2    ].b;
			r += data[(y * 2    )*w + x * 2 + 1].r;
			g += data[(y * 2    )*w + x * 2 + 1].g;
			b += data[(y * 2    )*w + x * 2 + 1].b;
			r += data[(y * 2 + 1)*w + x * 2    ].r;
			g += data[(y * 2 + 1)*w + x * 2    ].g;
			b += data[(y * 2 + 1)*w + x * 2    ].b;
			r += data[(y * 2 + 1)*w + x * 2 + 1].r;
			g += data[(y * 2 + 1)*w + x * 2 + 1].g;
			b += data[(y * 2 + 1)*w + x * 2 + 1].b;
			ndata[y * nw + x] = RGBA(r/4, g/4, b/4);
		}
	delete[] data;
	data = ndata;
	w = nw;
	h = nh;
}

void VImage::enlarge(float factor)
{
	int nw = w * factor;
	int nh = h * factor;
	RGBA * ndata = new RGBA[nw * nh];
	FOR(y, nh) FOR(x, nw) ndata[y * nw + x] = data[min(h - 1, int(y/factor)) * w + min(w - 1, int(x/factor))];
	delete[] data;
	data = ndata;
	w = nw;
	h = nh;
}

int VImage::sample(int x, int y, int size)
{
	if (size % 2 == 0) size++;
	int sum = 0;
	for (int dy = -size/2; dy <= size/2; dy++)
		for (int dx = -size/2; dx <= size/2; dx++)
			sum += !!getpixel(x + dx, y + dy).r;
	return sum > size*size/2;
}
void VImage::flipX()
{
	FOR(y, h) {
		int i = 0, j = w - 1;
		while (i < j) {
			swap(data[y * w + i], data[y * w + j]);
			i++;
			j--;
		}
	}
}

void VImage::gaussian_blur(int R)
{
	vector<float> C(2*R + 1);
	//
	float sum = 0;
	for (int x = -R; x <= R; x++)
		sum += C[x + R] = exp(-(x*x) / (2 * R * R)) / (2 * M_PI * R * R);
	float mult = 1.0 / sum;
	FOR(i, 2*R + 1)
		C[i] *= mult;
	// using fixed-point arithmetic for speed
	vector<int> IC(2 * R + 1);
	REP(i, C) IC[i] = (int) floor(0.5f + C[i] * 65536);
	//
	VImage temp = *this;
	RGBA* src = data, *dest = temp.data;
	// Separable gaussian blur - X direction
	FOR(y, h) FOR(x, w) {
		int r = 32768, g = 32768, b = 32768;
		for (int dx = -R; dx <= R; dx++) {
			if (x + dx < 0 || x + dx >= w) continue;
			const RGBA& pixel = src[y * w + (x + dx)];
			int coeff = IC[dx + R];
			r += pixel.r * coeff;
			g += pixel.g * coeff;
			b += pixel.b * coeff;
		}
		dest[y * w + x].r = r >> 16;
		dest[y * w + x].g = g >> 16;
		dest[y * w + x].b = b >> 16;
	}
	src = temp.data;
	dest = data;
	// Separable gaussian blur - Y direction
	FOR(y, h) FOR(x, w) {
		int r = 32768, g = 32768, b = 32768;
		for (int dy = -R; dy <= R; dy++) {
			if (y + dy < 0 || y + dy >= h) continue;
			const RGBA& pixel = src[(y + dy) * w + x];
			int coeff = IC[dy + R];
			r += pixel.r * coeff;
			g += pixel.g * coeff;
			b += pixel.b * coeff;
		}
		dest[y * w + x].r = r >> 16;
		dest[y * w + x].g = g >> 16;
		dest[y * w + x].b = b >> 16;
	}
}

void VImage::mark(int x, int y)
{
	RGBA& p = data[y * w + x];
	p.r = min(255, int(p.r) + 32);
	p.g = min(255, int(p.g) + 32);
}
