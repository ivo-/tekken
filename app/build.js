(function () {
    // Separate initial variant page into multiple A4 pages. `ctx`
    // should be some of the `.variant` nodes.
    var pagify = function(ctx) {
        var $page = $("section.page:last", ctx);
        var $extra = $("<section class='page'></section>");

        while ($page[0].clientHeight < $page[0].scrollHeight) {
            $extra.prepend($page.find("header:last").remove());
        }

        if ($extra.children().length) {
            $extra.insertAfter($page);
            pagify(ctx);
        }
    };

    window.start_build = function(callback) {
        $("#overlay").show();

        var $variants = $("#variants div.variant");

        $variants.each(function(){
            pagify(this);
        });

        var zip = new JSZip();
        var vcount = 0;

        $.map($variants, function(variant, i) {
            // Both test and answers pages should be included into the PDF.
            var $pages = $("section.page, section.answers-page", variant);

            var doc = new jsPDF("p", "mm", "a4");
            var pcount = 0;

            return $.map($pages, function(page){
                return html2canvas(page, {onrendered: function(canvas) {
                    // One page ready.
                    ++pcount;

                    doc.addImage(canvas.toDataURL("image/jpeg"), 0, 0, 210, 297);

                    if (pcount == $pages.length) {

                        // One variant ready.
                        ++vcount;

                        // DEBUG:
                        if (vcount == 1) doc.output('dataurlnewwindow');

                        zip.file(vcount + ".pdf", doc.output(), {binary: true});

                        if (vcount == $variants.length) {
                            saveAs(zip.generate({type:"blob"}), "test.zip");
                            $("#overlay").hide();
                            callback(true);
                        }
                    } else {
                        doc.addPage();
                    }
                }});
            });
        });
    };

})();
