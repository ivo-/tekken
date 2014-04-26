#include "types.h"
#include "vimage.h"

enum {
	ERR_DIM = -100,
	ERR_NO_SOLUTION,
	ERR_MANY_SOLUTIONS,
	NO_ERR = 1,
};

const double EPS = 1e-9;
inline bool eq(double a, double b) { return (fabs(a - b) < EPS); }

int gauss(vector<vector<double> > a, vector<double> b, vector<double>& result, bool allow_underdetermined = true)
{
	if (!a.size()) return ERR_DIM;
	int n = (int) a.size();
	if ((int) b.size() != n) return ERR_DIM;
	int m = (int) a[0].size();
	for (int i = 0; i < n; i++) if ((int) a[i].size() != m) return ERR_DIM;
	result.resize(m, 0.0);
	int r = 0, c = 0;
	while (r < n && c < m) {
		int bi = r;
		for (int i = r + 1; i < n; i++) if (fabs(a[i][c]) > fabs(a[bi][c])) bi = i;
		if (bi != r) {
			swap(a[r], a[bi]);
			swap(b[r], b[bi]);
		}
		if (eq(a[r][c], 0.0)) { c++; continue; }
		for (int i = r + 1; i < n; i++) {
			double mul = -a[i][c] / a[r][c];
			for (int j = 0; j < m; j++) a[i][j] += a[r][j] * mul;
			b[i] += b[r] * mul;
		}
		r++; c++;
	}
	if (r < n) {
		for (int i = r;  i < n; i++) if (!eq(b[i], 0.0)) return ERR_NO_SOLUTION;
		n = r;
	}
	if (r < c && !allow_underdetermined) return ERR_MANY_SOLUTIONS;
	for (int i = r - 1; i >= 0; i--) {
		int j = 0;
		while (eq(a[i][j], 0.0)) j++;
		double sum = 0;
		for (int k = j + 1; k < m; k++)
			sum += result[k] * a[i][k];;
		result[j] = (b[i] - sum) / a[i][j];
	}

	return NO_ERR;
}


// the idea is taken from OpenCV, modules/imgproc/src/imgwarp.cpp:3683
Matrix getPerspectiveTransform(const Pt src[], const Pt dest[])
{
	vector<vector<double> > M;
	for (int i = 0; i < 8; i++) M.push_back(vector<double>(8, 0.0));
	vector<double> b(8, 0.0);
	for (int i = 0; i < 4; i++ )
	{
		M[i][0] = M[i + 4][3] = src[i].x;
		M[i][1] = M[i + 4][4] = src[i].y;
		M[i][2] = M[i + 4][5] = 1;
		M[i][6] = -src[i].x * dest[i].x;
		M[i][7] = -src[i].y * dest[i].x;
		M[i + 4][6] = -src[i].x * dest[i].y;
		M[i + 4][7] = -src[i].y * dest[i].y;
		b[i] = dest[i].x;
		b[i + 4] = dest[i].y;
	}
	vector<double> result(8, 0.0);
	int res = gauss(M, b, result);
	if (res < 0) {
		printf("guass elimination error: %d\n", res);
	}
	Matrix ret;
	for (int i = 0; i < 8; i++) ret.m[i/3][i%3] = result[i];
	ret.m[2][2] = 1.0;
	return transposeMatrix(ret);
}

Pt transformPoint(const Pt& a, const Matrix& m)
{
	Vector t(a.x, a.y, 1.0);
	Vector tp = t * m;
	return Pt(tp.x / tp.z, tp.y / tp.z);
}

VImage transformPerspective(const VImage& src, Matrix m, int width, int height)
{
	VImage result(width, height);
	Matrix rm = inverseMatrix(m);
	for (int y = 0; y < height; y++)
		for (int x = 0; x < width; x++) {
			Vector t(x, y, 1);
			Vector tp = t * rm;
			int sx = (int) floor(tp.x / tp.z + 0.5);
			int sy = (int) floor(tp.y / tp.z + 0.5);
			result.putpixel(x, y, src.getpixel(sx, sy));
		}
	return result;
}


