
// var res = {...}; // JSON data
// var writer = new TemplateWriter()._2

var ResponseReceiver = Java.type('jp.co.bizreach.play2nashorn.ResponseReceiver');
print(ResponseReceiver.echo('Hello Echo'));

print(res2.toString());
print(res2.get("corporate").asText());

var render = function() {
  var partial1 = dust.compile('<p>some partial for {corporate}</p>', 'partial1');
  dust.loadSource(partial1);
  var partial2 = dust.compile('<div>PARTIAL: {>partial1/}</div>', 'partial2');
  dust.loadSource(partial2);
  var compiled = dust.compile(
    '<html><body><h1>企業名:{corporate}</h1><ol>{#jobs}<li>{.}</li>{/jobs}</ol>{>partial2/}</body></html>',
    "intro");
  dust.loadSource(compiled);
  dust.render("intro", res, function(err, out) {
    writer.flush(out);
  });
};

render();