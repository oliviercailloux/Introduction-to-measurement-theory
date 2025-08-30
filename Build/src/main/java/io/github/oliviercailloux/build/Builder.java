package io.github.oliviercailloux.build;

import static io.github.oliviercailloux.build.Resourcer.charSource;

import com.google.common.base.VerifyException;
import com.google.common.collect.ImmutableSet;
import com.google.common.io.CharSource;
import io.github.oliviercailloux.docbook.DocBookResources;
import io.github.oliviercailloux.jaris.xml.ConformityChecker;
import io.github.oliviercailloux.jaris.xml.KnownFactory;
import io.github.oliviercailloux.jaris.xml.XmlTransformer;
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

public class Builder implements AutoCloseable{
  @SuppressWarnings("unused")
  private static final Logger LOGGER = LoggerFactory.getLogger(Builder.class);
  
  private static final Path INPUT_DIR = Path.of("");
  private static final Path OUTPUT_DIR = Path.of("../Online Pages/");
  private static final CharSource STYLE = charSource("MyStyle.xsl");

  public static void main(String[] args) throws IOException {
    try (Builder builder = new Builder()) {
      builder.convert("Course");
      builder.convert("Ex1");
      builder.convert("Ex2");
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
    underlying.setURIResolver(DocBookResources.RESOLVER);
    try {
      transformer = XmlTransformerFactory.usingFactory(underlying).usingStylesheet(STYLE);
    } catch (IOException e) {
      throw new VerifyException(e);
    }
  }

  public void convert(String name) throws IOException {
    final Path adoc = INPUT_DIR.resolve("%s.adoc".formatted(name));
    if(!Files.exists(adoc)) LOGGER.info(Files.list(INPUT_DIR).collect(ImmutableSet.toImmutableSet()).toString());
    LOGGER.info("Converting {} to DocBook.", adoc);
    final String docBook = asciidoctor.convert(Files.readString(adoc),
        options);
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
