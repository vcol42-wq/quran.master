const { contextBridge, ipcRenderer } = require('electron');

contextBridge.exposeInMainWorld('electronAPI', {
  toggleMiniMode: (enable) => ipcRenderer.send('toggle-mini-mode', enable),
  minimize: () => ipcRenderer.send('window-minimize'),
  maximize: () => ipcRenderer.send('window-maximize'),
  close: () => ipcRenderer.send('window-close'),
  sendNotification: (title, body) => ipcRenderer.send('notify', { title, body }),
  openExternal: (url) => ipcRenderer.send('open-external', url),
  getAppVersion: () => '1.0.0'
});
