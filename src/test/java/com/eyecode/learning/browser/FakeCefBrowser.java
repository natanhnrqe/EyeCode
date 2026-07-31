package com.eyecode.learning.browser;

import org.cef.CefClient;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.browser.CefRequestContext;
import org.cef.callback.CefPdfPrintCallback;
import org.cef.callback.CefRunFileDialogCallback;
import org.cef.callback.CefStringVisitor;
import org.cef.handler.CefDialogHandler;
import org.cef.handler.CefDisplayHandler;
import org.cef.handler.CefDragHandler;
import org.cef.handler.CefFocusHandler;
import org.cef.handler.CefJSDialogHandler;
import org.cef.handler.CefKeyboardHandler;
import org.cef.handler.CefLoadHandler;
import org.cef.handler.CefRenderHandler;
import org.cef.handler.CefRequestHandler;
import org.cef.handler.CefWindowHandler;
import org.cef.misc.CefPdfPrintSettings;

import java.awt.Component;
import java.awt.Point;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

public final class FakeCefBrowser implements CefBrowser {

    private final List<String> loadedUrls = new ArrayList<>();
    private final List<String> executedJs = new ArrayList<>();
    private boolean closed;
    private boolean stopped;

    @Override public void createImmediately() {}
    @Override public Component getUIComponent() { return null; }
    @Override public CefClient getClient() { return null; }
    @Override public CefRequestContext getRequestContext() { return null; }
    @Override public CefRenderHandler getRenderHandler() { return null; }
    @Override public CefWindowHandler getWindowHandler() { return null; }
    @Override public boolean canGoBack() { return false; }
    @Override public void goBack() {}
    @Override public boolean canGoForward() { return false; }
    @Override public void goForward() {}
    @Override public boolean isLoading() { return false; }
    @Override public void reload() {}
    @Override public void reloadIgnoreCache() {}
    @Override public void stopLoad() { stopped = true; }
    @Override public int getIdentifier() { return 0; }
    @Override public CefFrame getMainFrame() { return null; }
    @Override public CefFrame getFocusedFrame() { return null; }
    @Override public CefFrame getFrameByIdentifier(String identifier) { return null; }
    @Override public CefFrame getFrameByName(String name) { return null; }
    @Override public Vector<String> getFrameIdentifiers() { return new Vector<>(); }
    @Override public Vector<String> getFrameNames() { return new Vector<>(); }
    @Override public int getFrameCount() { return 0; }
    @Override public boolean isPopup() { return false; }
    @Override public boolean hasDocument() { return true; }
    @Override public boolean isWindowless() { return false; }
    @Override public void viewSource() {}
    @Override public void getSource(CefStringVisitor visitor) {}
    @Override public void getText(CefStringVisitor visitor) {}
    @Override public void loadRequest(org.cef.network.CefRequest request) {}
    @Override public void loadURL(String url) { loadedUrls.add(url); }
    @Override public void executeJavaScript(String script, String url, int startLine) { executedJs.add(script); }
    @Override public String getURL() { return loadedUrls.isEmpty() ? "" : loadedUrls.get(loadedUrls.size() - 1); }
    @Override public void close(boolean force) { closed = true; }
    @Override public void setCloseAllowed() {}
    @Override public boolean doClose() { return false; }
    @Override public void onBeforeClose() {}
    @Override public boolean isClosing() { return closed; }
    @Override public boolean isClosed() { return closed; }
    @Override public void setFocus(boolean enable) {}
    @Override public void setWindowVisibility(boolean visible) {}
    @Override public double getZoomLevel() { return 0; }
    @Override public void setZoomLevel(double zoomLevel) {}
    @Override public void runFileDialog(CefDialogHandler.FileDialogMode mode, String title,
                                        String defaultFilePath, Vector<String> acceptFilters,
                                        CefRunFileDialogCallback callback) {}
    @Override public void startDownload(String url) {}
    @Override public void print() {}
    @Override public void printToPDF(String path, CefPdfPrintSettings settings, CefPdfPrintCallback callback) {}
    @Override public void find(String searchText, boolean forward, boolean matchCase, boolean findNext) {}
    @Override public void stopFinding(boolean clearSelection) {}
    @Override public void openDevTools() {}
    @Override public void openDevTools(Point inspectAt) {}
    @Override public void closeDevTools() {}
    @Override public org.cef.browser.CefDevToolsClient getDevToolsClient() { return null; }
    @Override public void replaceMisspelling(String word) {}
    @Override public void wasResized(int width, int height) {}
    @Override public void invalidate() {}
    @Override public void notifyScreenInfoChanged() {}
    @Override public void sendKeyEvent(KeyEvent e) {}
    @Override public void sendMouseEvent(MouseEvent e) {}
    @Override public void sendMouseWheelEvent(MouseWheelEvent e) {}
    @Override public void sendTouchEvent(org.cef.input.CefTouchEvent e) {}
    @Override public java.util.concurrent.CompletableFuture<java.awt.image.BufferedImage> createScreenshot(boolean arg) { return new java.util.concurrent.CompletableFuture<>(); }
    @Override public void ImeSetComposition(String text, java.util.List<org.cef.input.CefCompositionUnderline> underlines, org.cef.misc.CefRange selection, org.cef.misc.CefRange replacement) {}
    @Override public void ImeCommitText(String text, org.cef.misc.CefRange selection, int replacementRange) {}
    @Override public void ImeFinishComposingText(boolean keepSelection) {}
    @Override public void ImeCancelComposing() {}
    @Override public void setWindowlessFrameRate(int frameRate) {}
    @Override public java.util.concurrent.CompletableFuture<java.lang.Integer> getWindowlessFrameRate() { return new java.util.concurrent.CompletableFuture<>(); }
    public List<String> getLoadedUrls() { return loadedUrls; }
    public List<String> getExecutedJs() { return executedJs; }
    public boolean wasClosed() { return closed; }
    public boolean isStopped() { return stopped; }
}
