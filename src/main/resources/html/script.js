$(document).ready(function () {

    var container = $('#summary-container');

    var width = '600',
        height = '676',
        centered;

    var projection = d3.geo.mercator()
        .center([-68, -7])
        .scale([2000]);

    var color = d3.scale.linear()
        .domain([1, 100])
        .range(['orange', '#FF69B4']);


    var path = d3.geo.path()
        .projection(projection);

    var svg = d3.select("svg")
        .attr("width", width)
        .attr("height", height);

    //add background
    svg.append('rect')
        .attr('class', 'background')
        .attr('width', width)
        .attr('height', height)
        .on('click', clicked);

    var g = svg.append('g');
    var mapLayer = g.append('g').classed('map-layer', true);

    function mouseover(d) {
        d3.select(this).style('fill', 'green');
    }

    function mouseout(context, d, i) {
        if (centered && d === centered) {
            d3.select(context).style('green', color(i));
        }
        else {

            d3.select(context).style('fill', fillColor(d));
        }

    }

    function displayName(d) {
        console.log(d.properties.name_2)
    }

    function fillColor(d) {
        if (d.properties) {
            var fp = parseInt(d.properties.fp);
            var ppk = parseInt(d.properties.ppk);
            if (fp > ppk) {
                return color(1);
            } else if (fp < ppk) {
                return color(100)
            }
            else {
                return 'blue'
            }
        }
    }

    function clicked(d) {

        var x, y, k;


        // Compute centroid of the selected path
        if (d && centered !== d) {
            var centroid = path.centroid(d);
            x = centroid[0];
            y = centroid[1];
            k = 4;
            centered = d;
        } else {
            x = width / 2;
            y = height / 2;
            k = 1;
            centered = null;
        }

        if (centered && d === centered) {
            updateContainer(d);
            container.show();
        } else {
            container.hide();
        }

        mapLayer.selectAll('path')
            .style('fill', function (d) {
                return centered && d === centered ? 'green' : fillColor(d);
            });

        // Zoom
        g.transition()
            .duration(750)
            .attr('transform', 'translate(' + width / 2 + ',' + height / 2 + ')scale(' + k + ')translate(' + -x + ',' + -y + ')');
    }

    function updateContainer(d) {
        var total_votos = parseInt(d.properties.total);
        var ppk = d.properties.ppk;
        var fp = d.properties.fp;
        var impugnados = d.properties.impugnados;
        var nulos = d.properties.nulos;
        var per_ppk = ppk * 100 / total_votos;
        var per_fp = fp * 100 / total_votos;
        var per_nulos = nulos * 100 / total_votos;
        var per_impugnados = impugnados * 100 / total_votos;

        $('#nombre').text(d.properties.name);
        $('#ppk').text(ppk);
        $('#fp').text(fp);
        $('#inpug').text(impugnados);
        $('#null').text(nulos);
        $('#total').text(d.properties.total);
        $('#departamento').text(d.properties.departamento);

        $('#per-ppk').text(per_ppk);
        $('#per-fp').text(per_fp);
        $('#per-inpug').text(per_impugnados);
        $('#per-null').text(per_nulos);
        $('#per-total').text(100);

    }


    d3.json("../geojson/output/provincias_summary.json", function (error, json) {
        if (error) console.log(error);
        var features = json.features;
        console.log(features.length);
        mapLayer.selectAll("path")
            .data(features)
            .enter().append("path")
            .attr("d", path)
            .attr('vector-effect', 'non-scaling-stroke')
            .attr('fill', function (d) {
                return fillColor(d)
            })
            .on("mouseover", mouseover)
            .on("mouseout", function (d, i) {
                mouseout(this, d, i)
            })
            .on("click", clicked);
    });
});