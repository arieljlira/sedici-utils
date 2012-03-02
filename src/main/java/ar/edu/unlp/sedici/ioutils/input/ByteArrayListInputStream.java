/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ar.edu.unlp.sedici.ioutils.input;
 
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class ByteArrayListInputStream extends InputStream {

    private final List<byte[]> buffers = new ArrayList<byte[]>();
    private int currentBufferIndex = -1;
    private byte[] currentBuffer = null;
    private int pos = 0;
    
    public ByteArrayListInputStream(byte[] buffer) {
    	this(Collections.singletonList(buffer));
	}
    
    public ByteArrayListInputStream(Collection<byte[]> buffers) {
    	if (buffers != null)
    		this.buffers.addAll(buffers);
		nextBuffer();
	}
    
    public ByteArrayListInputStream(ByteArrayListInputStream masterInputStream) {
		this(masterInputStream.buffers);
	}

	@Override
    public int read() throws IOException {
    	if (!checkBuffer())
    		return -1;
    	
    	int ret = currentBuffer[pos++];
    	checkBuffer();
    	
    	return ret;
    }
    
    
	@Override
	public int available() throws IOException {
		int sum = -pos;
		for (byte[] bufI : this.buffers.subList(currentBufferIndex, buffers.size())) {
			sum += bufI.length;
		}
		return sum;
	}

	@Override
	public void close() throws IOException {
	}

	@Override
	public int read(byte[] b, int off, int len) throws IOException {
		if ((off < 0) 
                || (off > b.length) 
                || (len < 0) 
                || ((off + len) > b.length) 
                || ((off + len) < 0)) {
            throw new IndexOutOfBoundsException();
        } else if (len == 0) {
            return 0;
        } else if (!checkBuffer()) 
        	return -1;
        
		int remaining = len;
		while (checkBuffer() && remaining > 0 ){
			int part = Math.min(remaining, currentBuffer.length - pos);
			System.arraycopy(currentBuffer, pos, b, off + len - remaining, part);
			pos += part;
			remaining -= part;
		}
		
		return len - remaining;
        
	}

	@Override
	public int read(byte[] b) throws IOException {
		return this.read(b, 0, b.length);
	}

	@Override
	public synchronized void reset() throws IOException {
		this.currentBufferIndex = -1;
		nextBuffer();
	}

	@Override
	public long skip(long n) throws IOException {
		long remaining = n;
		while (checkBuffer() && remaining > 0 ){
			long part = Math.min(Math.min(remaining, currentBuffer.length - pos), 2048) ;
			pos += part;
			remaining -= part;
		}	
		return n - remaining;
	}

    
	///////////////////////////////////////////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////////////////////
	private void nextBuffer(){
		currentBufferIndex++;
    	pos = 0;
    	if (currentBufferIndex >= buffers.size())
    		currentBuffer = null;
    	else 
    		currentBuffer = buffers.get(currentBufferIndex);
	}
	
	private boolean checkBuffer() {
		if (currentBufferIndex >= buffers.size())
			return false;
		else if (pos >= currentBuffer.length ){
			this.nextBuffer();
	    	return checkBuffer();
		}else
			return true;
		
		
	}
	
	public static void main(String[] args) throws IOException {
		ByteArrayListInputStream b = new ByteArrayListInputStream("hello".getBytes());
		System.out.println("quedan " + b.available());
		byte[] buf = new byte[10];
		System.out.println("quedan " + b.read(buf) + " con contenido " + new String(buf));
		System.out.println("quedan " + b.read(buf) + " con contenido " + new String(buf));
	}
}
