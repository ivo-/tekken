void VImage::fix_falloff(void)
{
	const int N = 10;
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

	int total = w * h;
	for (int i = 0; i < total; i++)
		hist[data[i].r]++;
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
