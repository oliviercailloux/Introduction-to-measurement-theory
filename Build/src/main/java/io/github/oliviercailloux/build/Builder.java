package io.github.oliviercailloux.build;

import static io.github.oliviercailloux.build.Resourcer.charSource;

import com.google.common.io.CharSource;
import io.github.oliviercailloux.docbook.DocBookResources;
import io.github.oliviercailloux.jaris.xml.KnownFactory;
import io.github.oliviercailloux.jaris.xml.XmlTransformerFactory;
import io.github.oliviercailloux.publish.DocBookConformityChecker;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.xml.transform.TransformerFactory;
import org.asciidoctor.Asciidoctor;
import org.asciidoctor.Options;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Builder {
  @SuppressWarnings("unused")
  private static final Logger LOGGER = LoggerFactory.getLogger(Builder.class);

  public static void main(String[] args) throws Exception {
    new Builder().proceed();
  }

  public void proceed() throws IOException, ClassNotFoundException {
    LOGGER.info("Compiling.");
    final Path adoc = Path.of("../Course.adoc");
    final Path outputDir = Path.of("../../Online Pages/");

    final String docBook;
    try (Asciidoctor adocConverter = Asciidoctor.Factory.create()) {
      docBook = adocConverter.convert(Files.readString(adoc),
          Options.builder().standalone(true).backend("docbook").build());
    }
    DocBookConformityChecker.usingEmbeddedSchema().verifyValid(CharSource.wrap(docBook));
    Files.writeString(outputDir.resolve("out.dbk"), docBook);

    final CharSource stylesheet = charSource("MyStyle.xsl");

    TransformerFactory underlying = KnownFactory.SAXON.factory();
    underlying.setURIResolver(DocBookResources.RESOLVER);
    final XmlTransformerFactory transformerFactory = XmlTransformerFactory.usingFactory(underlying);

    final String html = transformerFactory.usingStylesheet(stylesheet).charsToChars(docBook);
    Files.writeString(outputDir.resolve("Course.html"), html);
  }
}
