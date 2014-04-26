#ifndef __PERSPECTIVE_H__
#define __PERSPECTIVE_H__


#include "types.h"
#include "vimage.h"

Matrix getPerspectiveTransform(const Pt src[], const Pt dest[]);
VImage transformPerspective(const VImage& src, Matrix m, int width, int height);
Pt transformPoint(const Pt& a, const Matrix& m);

//

#endif // __PERSPECTIVE_H__
