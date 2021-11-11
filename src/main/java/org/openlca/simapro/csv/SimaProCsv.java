package org.openlca.simapro.csv;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.function.Consumer;

import org.apache.commons.csv.CSVFormat;
import org.openlca.simapro.csv.method.ImpactMethodBlock;
import org.openlca.simapro.csv.process.ProcessBlock;
import org.openlca.simapro.csv.process.ProductStageBlock;
import org.openlca.simapro.csv.refdata.CalculatedParameterRow;
import org.openlca.simapro.csv.refdata.ElementaryFlowRow;
import org.openlca.simapro.csv.refdata.InputParameterRow;
import org.openlca.simapro.csv.refdata.LiteratureReferenceBlock;
import org.openlca.simapro.csv.refdata.QuantityRow;
import org.openlca.simapro.csv.refdata.SystemDescriptionBlock;
import org.openlca.simapro.csv.refdata.UnitBlock;
import org.openlca.simapro.csv.refdata.UnitRow;

public final class SimaProCsv {

  private SimaProCsv() {
  }

  /**
   * The default character set of SimaPro CSV is Windows 1252.
   *
   * @return the Windows 1252 character set
   */
  public static Charset defaultCharset() {
    return Charset.forName("windows-1252");
  }

  static CSVFormat formatOf(char delimiter) {
    return CSVFormat.Builder.create()
      .setDelimiter(delimiter)
      .setTrim(true)
      .setIgnoreEmptyLines(false)
      .setQuote('"')
      .setIgnoreSurroundingSpaces(true)
      .build();
  }

  public static Reader readerOf(File file) {
    return readerOf(file, defaultCharset());
  }

  public static Reader readerOf(File file, Charset charset) {
    if (file == null)
      return null;
    try {
      var stream = new FileInputStream(file);
      return readerOf(stream, charset);
    } catch (IOException e) {
      throw new RuntimeException("failed to read file: " + file, e);
    }
  }

  public static Reader readerOf(InputStream stream, Charset charset) {
    if (stream == null)
      return null;
    var cs = charset == null
      ? defaultCharset()
      : charset;
    return new InputStreamReader(stream, cs);
  }

  public static void read(File file, Consumer<CsvBlock> fn) {
    var header = CsvHeader.readFrom(file);
    try (var reader = readerOf(file)) {
      var iter = CsvLine.iter(header, reader);
      for (var line : iter) {

        if (line.first().equals("Process")) {
          var process = ProcessBlock.read(iter);
          fn.accept(process);
          continue;
        }

        if (line.first().equals("Product stage")) {
          var block = ProductStageBlock.read(iter);
          fn.accept(block);
          continue;
        }

        if (line.first().equals("Units")) {
          var units = UnitBlock.read(iter);
          fn.accept(units);
        }

      }

    } catch (IOException e) {
      throw new RuntimeException("failed to read blocks from file: " + file, e);
    }
  }

  public static CsvDataSet read(File file) {
    var header = CsvHeader.readFrom(file);
    try (var reader = SimaProCsv.readerOf(file)) {
      return read(header, reader);
    } catch (IOException e) {
      throw new RuntimeException("failed to read file: " + file, e);
    }
  }

  public static CsvDataSet read(CsvHeader header, Reader reader) {
    var dataSet = new CsvDataSet(header);
    var iter = CsvLine.iter(header, reader);
    var lines = iter.iterator();
    while (lines.hasNext()) {
      var line = lines.next();

      if (line.first().equals("Product stage")) {
        var block = ProductStageBlock.read(iter);
        dataSet.productStages().add(block);
        continue;
      }

      if (line.first().equals("Process")) {
        var block = ProcessBlock.read(iter);
        dataSet.processes().add(block);
        continue;
      }

      if (line.first().equals("System description")) {
        var block = SystemDescriptionBlock.read(iter);
        dataSet.systemDescriptions().add(block);
        continue;
      }

      if (line.first().equals("Method")) {
        var block = ImpactMethodBlock.read(iter);
        dataSet.methods().add(block);
        continue;
      }

      if (line.first().equals("Quantities")) {
        CsvLine.untilEmpty(lines, nextLine -> {
          var row = QuantityRow.read(nextLine);
          dataSet.quantities().add(row);
        });
        continue;
      }

      if (line.first().equals("Units")) {
        CsvLine.untilEmpty(lines, nextLine -> {
          var row = UnitRow.read(nextLine);
          dataSet.units().add(row);
        });
        continue;
      }

      if (line.first().equals("Raw materials")) {
        CsvLine.untilEmpty(lines, nextLine -> {
          var row = ElementaryFlowRow.read(nextLine);
          dataSet.rawMaterials().add(row);
        });
        continue;
      }

      if (line.first().equals("Airborne emissions")) {
        CsvLine.untilEmpty(lines, nextLine -> {
          var row = ElementaryFlowRow.read(nextLine);
          dataSet.airborneEmissions().add(row);
        });
        continue;
      }

      if (line.first().equals("Waterborne emissions")) {
        CsvLine.untilEmpty(lines, nextLine -> {
          var row = ElementaryFlowRow.read(nextLine);
          dataSet.waterborneEmissions().add(row);
        });
        continue;
      }

      if (line.first().equals("Final waste flows")) {
        CsvLine.untilEmpty(lines, nextLine -> {
          var row = ElementaryFlowRow.read(nextLine);
          dataSet.finalWasteFlows().add(row);
        });
        continue;
      }

      if (line.first().equals("Emissions to soil")) {
        CsvLine.untilEmpty(lines, nextLine -> {
          var row = ElementaryFlowRow.read(nextLine);
          dataSet.emissionsToSoil().add(row);
        });
        continue;
      }

      if (line.first().equals("Non material emissions")) {
        CsvLine.untilEmpty(lines, nextLine -> {
          var row = ElementaryFlowRow.read(nextLine);
          dataSet.nonMaterialEmissions().add(row);
        });
        continue;
      }

      if (line.first().equals("Social issues")) {
        CsvLine.untilEmpty(lines, nextLine -> {
          var row = ElementaryFlowRow.read(nextLine);
          dataSet.socialIssues().add(row);
        });
        continue;
      }

      if (line.first().equals("Economic issues")) {
        CsvLine.untilEmpty(lines, nextLine -> {
          var row = ElementaryFlowRow.read(nextLine);
          dataSet.economicIssues().add(row);
        });
        continue;
      }

      if (line.first().equals("Database Input parameters")) {
        CsvLine.untilEmpty(lines, nextLine -> {
          var row = InputParameterRow.read(nextLine);
          dataSet.databaseInputParameters().add(row);
        });
        continue;
      }

      if (line.first().equals("Database Calculated parameters")) {
        CsvLine.untilEmpty(lines, nextLine -> {
          var row = CalculatedParameterRow.read(nextLine);
          dataSet.databaseCalculatedParameters().add(row);
        });
        continue;
      }

      if (line.first().equals("Project Input parameters")) {
        CsvLine.untilEmpty(lines, nextLine -> {
          var row = InputParameterRow.read(nextLine);
          dataSet.projectInputParameters().add(row);
        });
        continue;
      }

      if (line.first().equals("Project Calculated parameters")) {
        CsvLine.untilEmpty(lines, nextLine -> {
          var row = CalculatedParameterRow.read(nextLine);
          dataSet.projectCalculatedParameters().add(row);
        });
        continue;
      }

      if (line.first().equals("Literature reference")) {
        var block = LiteratureReferenceBlock.read(iter);
        dataSet.literatureReferences().add(block);
      }
    }
    return dataSet;
  }
}
