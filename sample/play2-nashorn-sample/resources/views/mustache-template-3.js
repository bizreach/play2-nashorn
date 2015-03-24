var body = '<h1>Hello, Mustache 3</h1>';

var render = function() {
  return Mustache.render(page, res, {body:body});
};

render();