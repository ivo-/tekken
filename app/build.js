var pagify = function(ctx) {
    var page = $("section.page:last", ctx);
    var extra = $("<section class='page'></section>");

    while (page[0].clientHeight < page[0].scrollHeight) {
        extra.prepend(page.find("header:last").remove());
    }

    if (extra.children().length) {
        extra.insertAfter(page);
        pagify(ctx);
    }
};

var tekken_build = function(callback) {
    $("#overlay").show();
    var vcount = 0;
    var zip = new JSZip();
    var variants = $("#variants div.variant");

    variants.each(function(){
        pagify(this);
    });

    $.map(variants, function(variant, i) {
        var doc = new jsPDF("p", "mm", "a4");
        var pages = $("section.page, section.answers", variant);
        var pcount = 0;

        return $.map(pages, function(page){
            return html2canvas(page, {onrendered: function(canvas) {
                doc.addImage(canvas.toDataURL("image/jpeg"), 0, 0, 210, 297);
                doc.addPage();

                if (++pcount == pages.length) {
                    ++vcount;
                    zip.file(vcount + ".pdf", doc.output(), {binary: true});

                    if (vcount == variants.length) {
                        saveAs(zip.generate({type:"blob"}), "test.zip");
                        $("#overlay").hide();
                        callback(true);
                    }
                }
            }});
        });
    });
};
