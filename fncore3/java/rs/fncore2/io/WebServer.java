package rs.fncore2.io;

import org.nanohttpd.protocols.http.NanoHTTPD;
import org.nanohttpd.util.IHandler;
import rs.fncore2.io.handlers.*;
import java.io.IOException;

import org.nanohttpd.protocols.http.IHTTPSession;
import org.nanohttpd.protocols.http.response.Response;

public class WebServer implements IHandler<IHTTPSession, Response> {

	private NanoHTTPD _server;
	private E1C _e1c = new E1C();

	public WebServer() {
		_server = new NanoHTTPD(8080) {
		};
		_server.addHTTPInterceptor(this);
	}

	@Override
	public Response handle(IHTTPSession input) {
		if (input.getUri().startsWith("/api")) {
				return _e1c.handle(input);
		} 
		return null;
	}

	public void start() {
		try {
			_server.start();
		} catch (IOException ioe) {
		}
	}

	public void stop() {
		if (_server.isAlive())
			_server.stop();
	}

}
