


var render = function() {
  var template = '<h1>Hello, Mustache !</h1>';
  return Mustache.render(template, res, {partial1: partial1});
};

render();