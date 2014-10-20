function init(videoId) {
    SlideSynchronizer.playerId = videoId;
	SlideSynchronizer.render_slide(0);
	// preload all images
	try{
		for(var i=0;i<slides.length;i++) {
			var imgLoader = $('<img style="display:none;" src="' + slides[i] + '" />');
			$('#imgPreload').append(imgLoader);
		}
	}catch(exc){}
}
function on_spark_player_start() {
	SlideSynchronizer.start();
}

function on_spark_player_ready() {
    $(window).trigger("player__ready");
}

var SlideSynchronizer = {
    setTimes : function(t) {
    	this.TIMES = t;
	},
    start : function() {
        this.player = this.getPlayer();
        this.lastSlide = 0;
    	setInterval("SlideSynchronizer.syncTask()", 500);
	},
    syncTask : function() {
//        var ms = this.player.spark_player_position() * 1000;
//        $(window).trigger("player__playing", {
//            'position_in_ms': ms || 0
//        });
        try{
			var current_time = this.getTime();
            for(var i=0;i<this.TIMES.length;i++) {
                if(this.TIMES[i] > current_time) {
                    if(this.lastSlide != (i-1)) {
                        this.lastSlide = i-1;
                        this.render_slide(this.lastSlide);
                    }
                    break;
                }
            }
        }catch (err){}
	},
    render_slide : function (index) {
        var slide_path,
            swf_pattern;
        slide_path = slides[index];
        if (slide_path) {
            swf_pattern = /\.swf$/;
            if (swf_pattern.test(slide_path)) {
                this.replace_slide(slide_path);
                jQuery('#slideContainer').height(446);
            } else {
            	jQuery('#slideContainer').html('<div id="slide" style="visibility:visible;max-width:100%;max-height:100%;"><img style="height:100%;width:100%;" src="' + slide_path + '" /></div>');
            }
        }
    },
    
    replace_slide: function(slide_path){
    	// see : http://learnswfobject.com/advanced-topics/load-a-swf-using-javascript-onclick-event/ (to avoid mem leaks and performance problems: remove existing swf and then add new one when you replace swf)
		swfobject.removeSWF('#slideContainer');
    	jQuery('#slideContainer').html('<div id="slide"></div>');
    	swfobject.embedSWF(slide_path, 'slideContainer',
                "320", "265",
                "9.0.0", EXPRESSINSTALL_SWF, null,
                {'allowscriptaccess': 'always','allowfullscreen': 'true','wmode': 'opaque'});
    },

    getPlayer : function () {
   		return CKobject.getObjectById(this.playerId);
	},
    
    getTime : function(){
    	var a=this.player.getStatus();
		var ss='';
		for (var k in a){
			if(k=='time'){
				return a[k];
			}
		}
		return 0;
    }
};
