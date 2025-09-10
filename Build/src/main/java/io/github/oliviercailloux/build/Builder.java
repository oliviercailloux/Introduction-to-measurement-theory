package io.github.oliviercailloux.build;

import com.google.common.base.VerifyException;
import com.google.common.collect.ImmutableSet;
import com.google.common.io.CharSource;
import com.google.common.io.MoreFiles;
import io.github.oliviercailloux.docbook.DocBookResources;
import io.github.oliviercailloux.docbook.xslt3.DocBookXslt3Resources;
import io.github.oliviercailloux.jaris.xml.ConformityChecker;
import io.github.oliviercailloux.jaris.xml.KnownFactory;
import io.github.oliviercailloux.jaris.xml.XmlTransformer;
import io.github.oliviercailloux.jaris.xml.XmlTransformerFactory;
import io.github.oliviercailloux.publish.DocBookConformityChecker;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.xml.transform.TransformerFactory;
import org.asciidoctor.Asciidoctor;
import org.asciidoctor.Options;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Builder implements AutoCloseable {
  @SuppressWarnings("unused")
  private static final Logger LOGGER = LoggerFactory.getLogger(Builder.class);

  private static final Path INPUT_DIR = Path.of("");
  private static final Path OUTPUT_DIR = Path.of("../Online Pages/");
  // private static final CharSource STYLE = charSource("MyStyle.xsl");
  private static final URI STYLE = DocBookXslt3Resources.XSLT_3_HTML_URI;

  public static void main(String[] args) throws IOException {
    LOGGER.debug("CWD: {}.", Path.of(".").toAbsolutePath().normalize());
    try (Builder builder = new Builder()) {
      LOGGER.info("Clearing.");
      if (Files.exists(OUTPUT_DIR.resolve("css/"))) {
        MoreFiles.deleteRecursively(OUTPUT_DIR.resolve("css/"));
      }
      if (Files.exists(OUTPUT_DIR.resolve("js/"))) {
        MoreFiles.deleteRecursively(OUTPUT_DIR.resolve("js/"));
      }
      Files.deleteIfExists(OUTPUT_DIR.resolve("Course.html"));
      Files.deleteIfExists(OUTPUT_DIR.resolve("Ex1.html"));
      Files.deleteIfExists(OUTPUT_DIR.resolve("Ex2.html"));

      builder.convert("Course");
      builder.convert("Ex1");
      builder.convert("Ex2");

      LOGGER.debug("Copying resources.");
      DocBookXslt3Resources.copyResourcesTo(OUTPUT_DIR);
    }
  }

  private final Asciidoctor asciidoctor;
  private final Options options;
  private final ConformityChecker docBookChecker;
  private final XmlTransformer transformer;

  private Builder() {
    asciidoctor = Asciidoctor.Factory.create();
    options = Options.builder().standalone(true).backend("docbook").build();
    docBookChecker = DocBookConformityChecker.usingEmbeddedSchema();

    TransformerFactory underlying;
    try {
      underlying = KnownFactory.SAXON.factory();
    } catch (ClassNotFoundException e) {
      throw new VerifyException(e);
    }
    underlying.setURIResolver(DocBookResources.XML_RESOLVER.getURIResolver());
    transformer = XmlTransformerFactory.usingFactory(underlying).usingStylesheet(STYLE);
  }

  public void convert(String name) throws IOException {
    final Path adoc = INPUT_DIR.resolve("%s.adoc".formatted(name));
    if (!Files.exists(adoc)) {
      LOGGER.info(Files.list(INPUT_DIR).collect(ImmutableSet.toImmutableSet()).toString());
    }

    LOGGER.info("Converting {} to DocBook.", adoc);
    final String docBook = asciidoctor.convert(Files.readString(adoc), options);

    LOGGER.info("Validating DocBook.");
    docBookChecker.verifyValid(CharSource.wrap(docBook));
    // Files.writeString(OUTPUT_DIR.resolve("out.dbk"), docBook);

    LOGGER.info("Transforming DocBook to HTML.");
    final String html = transformer.charsToChars(docBook);
    Files.writeString(OUTPUT_DIR.resolve("%s.html".formatted(name)), html);
  }

  @Override
  public void close() {
    asciidoctor.close();
  }
}
