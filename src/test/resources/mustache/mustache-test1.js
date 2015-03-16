
// var res = {...};

var partial1 = '<p>some partial for {{corporate}}</p>';


var render = function() {
  var template = '<html><body><h1>企業名:{{corporate}}</h1><ol>{{#jobs}}<li>{{.}}</li>{{/jobs}}</ol>{{> partial1}}<br/>{{> partial2}}</body></html>';
  return Mustache.render(template, res, {partial1: partial1, partial2: partial2});
};

render();