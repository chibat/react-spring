package app;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import app.model.Request;
import app.model.Response;

@RestController
public class AppController {

  @PostMapping("/add")
  public ResponseEntity<Response> add(@RequestBody Request request) {
    var result = request.arg1() + request.arg2();
    var response = new Response(result);
    return ResponseEntity.ok(response);
  }

  // @GetMapping("/greet")
  // public ResponseEntity<String> greet(@RequestParam String name) {
  //   return ResponseEntity.ok("Hello " + name);
  // }
}

// $ curl -X POST -H "Content-Type: application/json" http://localhost:8080/add -d '{"arg1": 1, "arg2": 2}'
// $ curl -X POST -H "Content-Type: application/json" http://localhost:3000/add -d '{"arg1": 1, "arg2": 2}'

// operationId は、メソッド名をそのまま使う
