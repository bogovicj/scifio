/*
 * #%L
 * OME SCIFIO package for reading and converting scientific file formats.
 * %%
 * Copyright (C) 2005 - 2013 Open Microscopy Environment:
 *   - Board of Regents of the University of Wisconsin-Madison
 *   - Glencoe Software, Inc.
 *   - University of Dundee
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * 
 * The views and conclusions contained in the software and documentation are
 * those of the authors and should not be interpreted as representing official
 * policies, either expressed or implied, of any organization.
 * #L%
 */

package ome.scifio.io;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;

import org.scijava.Context;
import org.scijava.plugin.Plugin;

/**
 * Provides random access to URLs using the IRandomAccess interface.
 * Instances of URLHandle are read-only.
 *
 * <dl><dt><b>Source code:</b></dt>
 * <dd><a href="http://trac.openmicroscopy.org.uk/ome/browser/bioformats.git/components/common/src/loci/common/URLHandle.java">Trac</a>,
 * <a href="http://git.openmicroscopy.org/?p=bioformats.git;a=blob;f=components/common/src/loci/common/URLHandle.java;hb=HEAD">Gitweb</a></dd></dl>
 *
 * @see IRandomAccess
 * @see StreamHandle
 * @see java.net.URLConnection
 *
 * @author Melissa Linkert melissa at glencoesoftware.com
 */
@Plugin(type = IStreamAccess.class)
public class URLHandle extends StreamHandle {

  // -- Fields --

  /** URL of open socket */
  private String url;

  /** Socket underlying this stream */
  private URLConnection conn;

  // -- Constructors --

  /**
   * Zero-parameter constructor. This instructor can be used first
   * to see if a given file is constructable from this handle. If so,
   * setFile can then be used.
   */
  public URLHandle() {
    super();
  }
  
  public URLHandle(Context context) {
    super(context);
  }
  
  /**
   * Constructs a new URLHandle using the given URL.
   */
  public URLHandle(Context context, String url) throws IOException {
    super(context);
    setURL(url);
  }
  
  // -- URLHandle API --
  
  /**
   * Initializes this URLHandle with the provided url.
   * @throws IOException 
   */
  public void setURL(String url) throws IOException {
    if (!isConstructable(url)) {
      throw new HandleException(url + " is not a valid url.");
    }
    
    if (!url.startsWith("http") && !url.startsWith("file:")) {
      url = "http://" + url;
    }
    this.url = url;
    resetStream();
  }

  // -- IRandomAccess API methods --

  /* @see IRandomAccess#seek(long) */
  public void seek(long pos) throws IOException {
    if (pos < getFp() && pos >= getMark()) {
      getStream().reset();
      setFp(getMark());
      skip(pos - getFp());
    }
    else super.seek(pos);
  }
  
  // -- IStreamAccess API methods --
  
  /* @see IStreamAccess#isConstructable(String id) */
  public boolean isConstructable(String id) throws IOException {
    return id.startsWith("http:") || id.startsWith("file:");
  }

  // -- StreamHandle API methods --

  /* @see IStreamAccess#resetStream() */
  public void resetStream() throws IOException {
    conn = (new URL(url)).openConnection();
    setStream(new DataInputStream(new BufferedInputStream(
      conn.getInputStream(), RandomAccessInputStream.MAX_OVERHEAD)));
    setFp(0);
    setMark(0);
    setLength(conn.getContentLength());
    if (getStream() != null) getStream().mark(RandomAccessInputStream.MAX_OVERHEAD);
  }

  // -- Helper methods --

  /** Skip over the given number of bytes. */
  private void skip(long bytes) throws IOException {
    while (bytes >= Integer.MAX_VALUE) {
      bytes -= skipBytes(Integer.MAX_VALUE);
    }
    int skipped = skipBytes((int) bytes);
    while (skipped < bytes) {
      int n = skipBytes((int) (bytes - skipped));
      if (n == 0) break;
      skipped += n;
    }
  }
}
