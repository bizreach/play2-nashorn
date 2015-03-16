var body = '<h1>Hello, Mustache 2</h1>';

var render = function() {
  return Mustache.render(page, res, {header:header, body:body});
};

render();