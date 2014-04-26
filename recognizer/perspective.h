Matrix getPerspectiveTransform(const Pt src[], const Pt dest[]);
VImage transformPerspective(const VImage& src, Matrix m, int width, int height);
Pt transformPoint(const Pt& a, const Matrix& m);

//

