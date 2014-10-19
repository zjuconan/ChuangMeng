define(['jquery'],function(require, exports, module){
    var $ = require('jquery');
    function animate($target,callback){
        var n    = 1,
            step = 13,
            time = parseInt(800/step);

        var of = this.offset(),
            offset = $target.offset();
        var x  = (offset.left - of.left)/time,
            y  = (offset.top - of.top)/time;

        if(x == 0 && y == 0)return;

        var self = this;

        var interval = setInterval(function(){
            if(n > time){
                clearInterval(interval);
                callback && callback.call(self);
            }else{
                self.offset({left: of.left + x*n, top:of.top + y*n});
            }
            n++;
        },step);
    }
    $.fn.addMP = function(selector,callback){
        this.each(function(){
            animate.call($(this),$(selector),callback);
        });
    }
});