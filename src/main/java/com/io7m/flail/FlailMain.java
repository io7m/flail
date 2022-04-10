/*
 * Copyright © 2018 Mark Raynsford <code@io7m.com> https://www.io7m.com
 *
 * Permission to use, copy, modify, and/or distribute this software for any
 * purpose with or without fee is hereby granted, provided that the above
 * copyright notice and this permission notice appear in all copies.
 *
 * THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES
 * WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY
 * SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
 * WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
 * ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR
 * IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
 */

package com.io7m.flail;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xbill.DNS.DClass;
import org.xbill.DNS.Message;
import org.xbill.DNS.Name;
import org.xbill.DNS.Record;
import org.xbill.DNS.SimpleResolver;
import org.xbill.DNS.Type;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.util.List;
import java.util.stream.Collectors;

/**
 * DNS stress-testing tool.
 */

public final class FlailMain
{
  private static final Logger LOG = LoggerFactory.getLogger(FlailMain.class);

  private FlailMain()
  {

  }

  /**
   * Command-line entry point.
   *
   * @param args Command-line arguments
   *
   * @throws IOException On I/O exceptions
   */

  public static void main(
    final String[] args)
    throws IOException
  {
    if (args.length < 3) {
      LOG.error("usage: names.txt server-address server-port");
      System.exit(1);
    }

    final Path names = Paths.get(args[0]);

    final InetSocketAddress server_address;
    try {
      server_address =
        new InetSocketAddress(args[1], Integer.parseUnsignedInt(args[2]));
    } catch (final NumberFormatException e) {
      LOG.error("could not parse port number: {}", e.getMessage());
      LOG.error("usage: names.txt server-address server-port");
      System.exit(1);
      return;
    }

    final List<String> lines =
      Files.lines(names)
        .map(String::trim)
        .filter(s -> !s.isEmpty())
        .collect(Collectors.toList());

    final SecureRandom rng = new SecureRandom();

    final SimpleResolver resolver = new SimpleResolver();
    resolver.setAddress(server_address);
    resolver.setTCP(false);

    long requests = 0L;
    long successes = 0L;
    long failures = 0L;

    while (true) {
      final String name = lines.get(rng.nextInt(lines.size()));
      ++requests;

      final Record record =
        Record.newRecord(Name.fromString(name, Name.root), Type.A, DClass.IN);
      final Message query =
        Message.newQuery(record);

      try {
        resolver.send(query);
        ++successes;
      } catch (final IOException e) {
        LOG.error("{}: {}", name, e.getMessage(), e);
        ++failures;
      }

      try {
        Thread.sleep(5L);
      } catch (final InterruptedException e) {
        Thread.currentThread().interrupt();
      }

      if (requests % 10L == 0L) {
        LOG.info(
          "requests: {}/{}/{} (total/successes/failures)",
          Long.valueOf(requests),
          Long.valueOf(successes),
          Long.valueOf(failures));
      }
    }
  }
}
