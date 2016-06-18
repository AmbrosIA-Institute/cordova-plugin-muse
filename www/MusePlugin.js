var exec = require('cordova/exec');

function MusePlugin() { 
}

MusePlugin.prototype = {
    constructor: MusePlugin,

    refresh: function(success, failure) { 
        exec(success, failure, "MusePlugin", "refresh", []);
    },

    list: function(success, failure) { 
        exec(success, failure, "MusePlugin", "list", []);
    },

    connect: function(device, success, failure) {
        args = typeof device !== 'undefined' ? [device] : [];
        exec(success, failure, "MusePlugin", "connect", args);
    },

    connect_index: function(device, success, failure) {
        args = typeof device !== 'undefined' ? [device] : [];
        exec(success, failure, "MusePlugin", "connect_index", args);
    },

    connect_mac: function(device, success, failure) {
        args = typeof device !== 'undefined' ? [device] : [];
        exec(success, failure, "MusePlugin", "connect_mac", args);
    },

    disconnect: function(device, success, failure) {
        args = typeof device !== 'undefined' ? [device] : [];
        exec(success, failure, "MusePlugin", "disconnect", args);
    },

    disconnect_index: function(device, success, failure) {
        args = typeof device !== 'undefined' ? [device] : [];
        exec(success, failure, "MusePlugin", "disconnect_index", args);
    },

    disconnect_mac: function(device, success, failure) {
        args = typeof device !== 'undefined' ? [device] : [];
        exec(success, failure, "MusePlugin", "disconnect_mac", args);
    },

    disconnect_name: function(device, success, failure) {
        args = typeof device !== 'undefined' ? [device] : [];
        exec(success, failure, "MusePlugin", "disconnect_name", args);
    },

    listen: function(device, success, failure) {
        args = typeof device !== 'undefined' ? [device] : [];
        exec(success, failure, "MusePlugin", "listen", args);
    },

    listen_index: function(device, success, failure) {
        args = typeof device !== 'undefined' ? [device] : [];
        exec(success, failure, "MusePlugin", "listen_index", args);
    },

    listen_mac: function(device, success, failure) {
        args = typeof device !== 'undefined' ? [device] : [];
        exec(success, failure, "MusePlugin", "listen_mac", args);
    },

    listen_name: function(device, success, failure) {
        args = typeof device !== 'undefined' ? [device] : [];
        exec(success, failure, "MusePlugin", "listen_name", args);
    }

}

var musePlugin = new MusePlugin();
module.exports = musePlugin;
