# TEKKEN

## Notes

- Building the code

* `lein cljsbuild once dev`
* `lein cljsbuild auto dev`

- Start the project

* Build the code
* `open resources/index.html`

- Interop

```clojure
;; Access global variables
js/$
js/document
js/document.body.firstChild

;; Get property
(.-firstChild js/document)

;; Get/Set property
(aget js/document "title")
(aset js/document "title" "New")

;; Invoke method
(js/alert 10)
(. js/document getElementById "my-app")
(. alert js/window "Call method.")

;; Make native js class
(js/Array.)
```

## License

The MIT License (MIT)

Copyright REPL5 (c) 2014 <copyright holders>

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.
