typedef unsigned char uint8;
typedef unsigned uint32;
typedef long long ll;
typedef unsigned long long ull;

struct Pt {
	double x, y;
	Pt() {}
	Pt(double x, double y): x(x), y(y) {}
	Pt(const std::pair<int, int>& p): x(p.first), y(p.second) {}
	inline double lengthSqr() const { return x*x + y*y; }
	inline double length() const { return hypot(x, y); }
	void operator *= (double m)
	{
		x *= m; y *= m;
	}
	void operator += (const Pt& rhs)
	{
		x += rhs.x; y += rhs.y;
	}
	void round(int& sx, int& sy) const
	{
		sx = (int) floor(x + 0.5);
		sy = (int) floor(y + 0.5);
	}
	void normalize() { (*this) *= 1.0 / length(); }
};

inline Pt operator + (const Pt& a, const Pt& b)
{
	return Pt(a.x + b.x, a.y + b.y);
}

inline Pt operator - (const Pt& a, const Pt& b)
{
	return Pt(a.x - b.x, a.y - b.y);
}

inline double operator * (const Pt& a, const Pt& b)
{
	return (a.x * b.x + a.y * b.y);
}

inline Pt operator * (const Pt& a, double m)
{
	return Pt(a.x * m, a.y * m);
}

inline Pt operator / (const Pt& a, double m)
{
	return Pt(a.x / m, a.y / m);
}

inline Pt operator * (double m, const Pt& a)
{
	return Pt(a.x * m, a.y * m);
}


// End file `types.h':
// Start file `linear_algebra.h':


struct Vector {
	double x, y, z;

	Vector () {}
	Vector(double _x, double _y, double _z) { set(_x, _y, _z); }
	void set(double _x, double _y, double _z)
	{
		x = _x;
		y = _y;
		z = _z;
	}
	void makeZero(void)
	{
		x = y = z = 0.0;
	}
	inline double length(void) const
	{
		return sqrt(x * x + y * y + z * z);
	}
	inline double lengthSqr(void) const
	{
		return (x * x + y * y + z * z);
	}
	inline void scale(double multiplier)
	{
		x *= multiplier;
		y *= multiplier;
		z *= multiplier;
	}
	void operator *= (double multiplier)
	{
		scale(multiplier);
	}
	void operator /= (double divider)
	{
		scale(1.0 / divider);
	}
	inline void normalize(void)
	{
		double lSqr = (x * x + y * y + z * z);
		if (lSqr >= 0.999999 && lSqr < 1.000001) return;
		scale(1.0 / sqrt(lSqr));
	}
	void setLength(double newLength)
	{
		scale(newLength / length());
	}
	void print() const { printf("(%.3lf, %.3lf, %.3lf)", x, y, z); }
	void println() const { printf("(%.3lf, %.3lf, %.3lf)\n", x, y, z); }
	inline double& operator[] (int i)
	{
		return (&x)[i];
	}
	inline const double& operator[] (int i) const
	{
		return (&x)[i];
	}
	int maxDimension() const
	{
		int bi = 0;
		double maxD = fabs(x);
		if (fabs(y) > maxD) { maxD = fabs(y); bi = 1; }
		if (fabs(z) > maxD) { maxD = fabs(z); bi = 2; }
		return bi;
	}
	void operator += (const Vector& rhs) { x += rhs.x; y += rhs.y; z += rhs.z; }
	void operator -= (const Vector& rhs) { x -= rhs.x; y -= rhs.y; z -= rhs.z; }
};

inline Vector operator + (const Vector& a, const Vector& b)
{
	return Vector(a.x + b.x, a.y + b.y, a.z + b.z);
}

inline Vector operator - (const Vector& a, const Vector& b)
{
	return Vector(a.x - b.x, a.y - b.y, a.z - b.z);
}

inline Vector operator - (const Vector& a)
{
	return Vector(-a.x, -a.y, -a.z);
}

/// dot product
inline double operator * (const Vector& a, const Vector& b)
{
	return a.x * b.x + a.y * b.y + a.z * b.z;
}
/// dot product (functional form, to make it more explicit):
inline double dot(const Vector& a, const Vector& b)
{
	return a.x * b.x + a.y * b.y + a.z * b.z;
}
/// cross product
inline Vector operator ^ (const Vector& a, const Vector& b)
{
	return Vector(
		a.y * b.z - a.z * b.y,
		a.z * b.x - a.x * b.z,
		a.x * b.y - a.y * b.x
	);
}

inline Vector operator * (const Vector& a, double multiplier)
{
	return Vector(a.x * multiplier, a.y * multiplier, a.z * multiplier);
}
inline Vector operator * (double multiplier, const Vector& a)
{
	return Vector(a.x * multiplier, a.y * multiplier, a.z * multiplier);
}
inline Vector operator / (const Vector& a, double divider)
{
	double multiplier = 1.0 / divider;
	return Vector(a.x * multiplier, a.y * multiplier, a.z * multiplier);
}

struct Matrix {
	double m[3][3];
	Matrix() {}
	Matrix(double diagonalElement)
	{
		for (int i = 0; i < 3; i++)
			for (int j = 0; j < 3; j++)
				if (i == j) m[i][j] = diagonalElement;
				else m[i][j] = 0.0;
	}
};

inline Vector operator * (const Vector& v, const Matrix& m)
{
	return Vector(
		v.x * m.m[0][0] + v.y * m.m[1][0] + v.z * m.m[2][0],
		v.x * m.m[0][1] + v.y * m.m[1][1] + v.z * m.m[2][1],
		v.x * m.m[0][2] + v.y * m.m[1][2] + v.z * m.m[2][2]
	);
}

inline void operator *= (Vector& v, const Matrix& a) { v = v*a; }

inline Matrix operator * (const Matrix& a, const Matrix& b)
{
	Matrix c(0.0);
	for (int i = 0; i < 3; i++)
		for (int j = 0; j < 3; j++)
			for (int k = 0; k < 3; k++)
				c.m[i][j] += a.m[i][k] * b.m[k][j];
	return c;
}

static double cofactor(const Matrix& m, int ii, int jj)
{
	int rows[2], rc = 0, cols[2], cc = 0;
	for (int i = 0; i < 3; i++)
		if (i != ii) rows[rc++] = i;
	for (int j = 0; j < 3; j++)
		if (j != jj) cols[cc++] = j;
	double t = m.m[rows[0]][cols[0]] * m.m[rows[1]][cols[1]] - m.m[rows[1]][cols[0]] * m.m[rows[0]][cols[1]];
	if ((ii + jj) % 2) t = -t;
	return t;
}

inline double determinant(const Matrix& a)
{
	return a.m[0][0] * a.m[1][1] * a.m[2][2]
	     - a.m[0][0] * a.m[1][2] * a.m[2][1]
	     - a.m[0][1] * a.m[1][0] * a.m[2][2]
	     + a.m[0][1] * a.m[1][2] * a.m[2][0]
	     + a.m[0][2] * a.m[1][0] * a.m[2][1]
	     - a.m[0][2] * a.m[1][1] * a.m[2][0];
}

inline Matrix inverseMatrix(const Matrix& m)
{
	double D = determinant(m);
	if (fabs(D) < 1e-12) return m; // an error; matrix is not invertible
	double rD = 1.0 / D;
	Matrix result;
	for (int i = 0; i < 3; i++)
		for (int j = 0; j < 3; j++)
			result.m[i][j] = rD * cofactor(m, j, i);
	return result;
}

inline Matrix transposeMatrix(const Matrix& a)
{
	Matrix result;
	for (int i = 0; i < 3; i++)
		for (int j = 0; j < 3; j++)
			result.m[i][j] = a.m[j][i];
	return result;
}

struct RGBA {
	uint8 r, g, b, a;
	RGBA() {}
	explicit RGBA(unsigned t)
	{
		b = t & 0xff; t >>= 8;
		g = t & 0xff; t >>= 8;
		r = t & 0xff; t >>= 8;
		a = t;
	}
	RGBA(uint8 r, uint8 g, uint8 b, uint8 a = 0): r(r), g(g), b(b), a(a)
	{}
	inline uint32 u32() const
	{
		return (uint32) r | ((uint32) g << 8) | ((uint32) b << 16) | ((uint32) a << 24);
	}
	inline uint32 u24() const
	{
		return (uint32) r | ((uint32) g << 8) | ((uint32) b << 16);
	}
	inline unsigned sum() const { return (unsigned) r + (unsigned) g + (unsigned) b; }
};


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
	void resizeHalf(void);
};
