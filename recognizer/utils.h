#ifndef __UTILS_H__
#define __UTILS_H__

#include <wx/string.h>

#define FOR(i, n) for (int i = 0; i < (int) n; i++)
#define REP(i, vec) for (int i = 0; i < (int) vec.size(); i++)
#define COUNT_OF(arr) ((sizeof(arr) / sizeof(arr[0])))

template <typename T>
inline T sqr(T x) { return x*x; }

inline wxString WXSTRING(const string& s)
{
	return wxString(s.c_str(), wxConvLibc);
}

#endif // __UTILS_H__
