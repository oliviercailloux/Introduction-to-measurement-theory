package io.github.oliviercailloux.build;

import com.google.common.base.VerifyException;
import com.google.common.collect.ImmutableSet;
import com.google.common.io.CharSource;
import com.google.common.io.MoreFiles;
import io.github.oliviercailloux.docbook.DocBookResources;
import io.github.oliviercailloux.docbook.xslt1.DocBookXslt1Resources;
import io.github.oliviercailloux.docbook.xslt3.DocBookXslt3Resources;
import io.github.oliviercailloux.jaris.xml.ConformityChecker;
import io.github.oliviercailloux.jaris.xml.KnownFactory;
import io.github.oliviercailloux.jaris.xml.XmlTransformer;
import io.github.oliviercailloux.jaris.xml.XmlTransformerFactory;
import io.github.oliviercailloux.publish.DocBookConformityChecker;
import io.github.oliviercailloux.publish.FoToPdfTransformer;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.xml.transform.TransformerFactory;
import org.asciidoctor.Asciidoctor;
import org.asciidoctor.Attributes;
import org.asciidoctor.Options;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Builder implements AutoCloseable {
  @SuppressWarnings("unused")
  private static final Logger LOGGER = LoggerFactory.getLogger(Builder.class);

  private static final Path INPUT_DIR = Path.of("..");
  private static final Path OUTPUT_DIR = Path.of("../../Online Pages/");
  private static final Path OUTPUT_LOCAL_DIR = Path.of("../../Local/");
  private static final CharSource TO_HTML_MY_STYLE = Resourcer.charSource("MyStyle.xsl");
  private static final URI TO_FO_STYLE = DocBookXslt1Resources.XSLT_1_FO_URI;
  private static final URI TO_HTML_STYLE = DocBookXslt3Resources.XSLT_3_HTML_URI;

  public static void main(String[] args) throws Exception {
    LOGGER.debug("CWD: {}.", Path.of(".").toAbsolutePath().normalize());
    try (Builder builder = new Builder()) {
      LOGGER.info("Clearing.");
      if (Files.exists(OUTPUT_DIR.resolve("css/"))) {
        MoreFiles.deleteRecursively(OUTPUT_DIR.resolve("css/"));
      }
      if (Files.exists(OUTPUT_DIR.resolve("js/"))) {
        MoreFiles.deleteRecursively(OUTPUT_DIR.resolve("js/"));
      }
      if (Files.exists(OUTPUT_LOCAL_DIR.resolve("css/"))) {
        MoreFiles.deleteRecursively(OUTPUT_LOCAL_DIR.resolve("css/"));
      }
      if (Files.exists(OUTPUT_LOCAL_DIR.resolve("js/"))) {
        MoreFiles.deleteRecursively(OUTPUT_LOCAL_DIR.resolve("js/"));
      }
      Files.deleteIfExists(OUTPUT_DIR.resolve("Course.fo"));
      Files.deleteIfExists(OUTPUT_DIR.resolve("Ex1.fo"));
      Files.deleteIfExists(OUTPUT_DIR.resolve("Ex2.fo"));
      Files.deleteIfExists(OUTPUT_DIR.resolve("Sol1.fo"));
      Files.deleteIfExists(OUTPUT_DIR.resolve("Course.html"));
      Files.deleteIfExists(OUTPUT_DIR.resolve("Ex1.html"));
      Files.deleteIfExists(OUTPUT_DIR.resolve("Ex2.html"));
      Files.deleteIfExists(OUTPUT_DIR.resolve("Sol1.html"));
      Files.deleteIfExists(OUTPUT_DIR.resolve("Exam1.fo"));
      Files.deleteIfExists(OUTPUT_DIR.resolve("Exam1.html"));

      builder.convertAll();

      LOGGER.debug("Copying resources.");
      DocBookXslt3Resources.copyResourcesTo(OUTPUT_DIR);
      DocBookXslt3Resources.copyResourcesTo(OUTPUT_LOCAL_DIR);
    }
  }

  private final Asciidoctor asciidoctor;
  private final Options options;
  private final ConformityChecker docBookChecker;
  private final XmlTransformerFactory factory;
  private final XmlTransformer toFo;
  private final FoToPdfTransformer toPdf;
  private final XmlTransformer toHtml;

  private Builder() {
    asciidoctor = Asciidoctor.Factory.create();
    Attributes attributes = Attributes.builder().attribute("relfilesuffix", ".html").build();
    options = Options.builder().standalone(true).backend("docbook").attributes(attributes).build();
    docBookChecker = DocBookConformityChecker.usingEmbeddedSchema();

    TransformerFactory underlying;
    try {
      underlying = KnownFactory.SAXON.factory();
    } catch (ClassNotFoundException e) {
      throw new VerifyException(e);
    }
    underlying.setURIResolver(DocBookResources.XML_RESOLVER.getURIResolver());
    factory = XmlTransformerFactory.usingFactory(underlying);
    toFo = factory.usingStylesheet(TO_FO_STYLE);
    toPdf = FoToPdfTransformer.usingFactory(underlying);
    toHtml = factory.usingStylesheet(TO_HTML_STYLE);
  }

  public void convertAll() throws IOException {
    convert("Course");
    convert("Ex1");
    convert("Ex2");
    convert("Sol1");
    convert("Exam1", OUTPUT_DIR, factory.usingStylesheet(TO_HTML_MY_STYLE));
  }

  public void convert(String name) throws IOException {
    convert(name, OUTPUT_DIR, toHtml);
  }

  public void convert(String name, Path outputDir, XmlTransformer transformer) throws IOException {
    final Path adoc = INPUT_DIR.resolve("%s.adoc".formatted(name));
    if (!Files.exists(adoc)) {
      LOGGER.info(Files.list(INPUT_DIR).collect(ImmutableSet.toImmutableSet()).toString());
    }

    LOGGER.info("Converting {} to DocBook.", adoc);
    final String docBook = asciidoctor.convert(Files.readString(adoc), options);

    LOGGER.info("Validating DocBook.");
    docBookChecker.verifyValid(CharSource.wrap(docBook));
    // Files.writeString(OUTPUT_DIR.resolve("out.dbk"), docBook);

    // LOGGER.info("Transforming DocBook to FO.");
    // final String fo = toFo.charsToChars(docBook);
    // Files.writeString(OUTPUT_DIR.resolve("%s.fo".formatted(name)), fo);

    // LOGGER.info("Transforming FO to PDF.");
    // final byte[] pdf = toPdf.charsToBytes(fo);
    // Files.write(OUTPUT_DIR.resolve("%s.pdf".formatted(name)), pdf);

    LOGGER.info("Transforming DocBook to HTML.");
    final String html = transformer.charsToChars(docBook);
    Files.writeString(outputDir.resolve("%s.html".formatted(name)), html);
  }

  @Override
  public void close() {
    asciidoctor.close();
  }
}
