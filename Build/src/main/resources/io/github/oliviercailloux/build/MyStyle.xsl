<?xml version='1.0'?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  xmlns:m="http://docbook.org/ns/docbook/modes"
  version="3.0">
  <xsl:import href="https://cdn.docbook.org/release/xsltng/2.6.0/xslt/docbook.xsl" />
<xsl:output
    method="html" encoding="UTF-8" indent="no" />
<xsl:template match="*" mode="m:toc" />
</xsl:stylesheet>