package app;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.BufferedWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultHandler;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest(classes = Application.class)
public class OpenApiDocsGenerator {
	@Autowired
	private WebApplicationContext context;

	private MockMvc mockMvc;

	@BeforeEach
	public void setUp() {
		this.mockMvc = MockMvcBuilders.webAppContextSetup(this.context).build();
	}

	@Test
	public void convertSwaggerToAsciiDoc() throws Exception {
		this.mockMvc.perform(get("/v3/api-docs").accept("application/json;charset=UTF-8"))
				.andDo(new Handler()).andExpect(status().isOk());
	}

	public static class Handler implements ResultHandler {

		private final String outputDir = "build";
		private final String fileName = "openapi.json";

		@Override
		public void handle(MvcResult result) throws Exception {
			MockHttpServletResponse response = result.getResponse();
			String swaggerJson = response.getContentAsString();
			Files.createDirectories(Paths.get(outputDir));
			try (BufferedWriter writer = Files.newBufferedWriter(
					Paths.get(outputDir, fileName), StandardCharsets.UTF_8)) {
				writer.write(swaggerJson);
			}
		}
	}
}
