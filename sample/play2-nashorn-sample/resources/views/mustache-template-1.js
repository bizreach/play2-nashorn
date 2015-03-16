
var render = function() {
  var template = '<html><body><h1>Hello, Mustache 1 !</h1></body></html>';
  return Mustache.render(template, res, {});
};

render();