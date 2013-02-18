/**
 * UIActivityUpdates.js
 */
(function ($){
	var UIActivityUpdates = {
	  numberOfUpdatedActivities: 0,
	  cookieName: '',
	  noUpdates: '',
	  currentRemoteId : '',
	  lastUpdatedActivitiesNumKey : '',
	  clientTimerAtStart: 0,
	  currentServerTime : 0,
	  isOnMyActivities: false,
	  ALL : 'ALL_ACTIVITIES',
	  CONNECTIONS : 'CONNECTIONS',
	  MY_SPACES : 'MY_SPACE',
	  MY_ACTIVITIES : 'MY_ACTIVITIES',
    CURRENT_SELECTED_TAB_KEY : "exo_social_activity_stream_tab_selected_%remoteId%",
    LAST_VISTED_TIME_FROM: "exo_social_activity_stream_%tab%_visited_%remoteId%_from",
    LAST_VISTED_TIME_OLD_FROM: "exo_social_activity_stream_%tab%_visited_%remoteId%_old_from",
    LAST_VISTED_TIME_TO: "exo_social_activity_stream_%tab%_visited_%remoteId%_to",
    LAST_UPDATED_ACTIVITIES_NUM : "exo_social_last_updated_activities_num_on_%tab%_of_%remoteId%",
    REMOTE_ID_PART: "%remoteId%",
    TAB_PART: "%tab%",
    setCookies : function(name, value, expiredays) {
      var exdate = new Date();
      exdate.setDate(exdate.getDate() + expiredays);
      expiredays = ((expiredays == null) ? "" : ";expires=" + exdate.toGMTString());
      var path = ';path=/portal';
      document.cookie = name + "=" + escape(value) + expiredays + path;
    },
	  resetCookie: function(cookieKey, value) {
     	console.log('resetCookie');
	    cookieKey = $.trim(cookieKey); 
	    UIActivityUpdates.setCookies(cookieKey, '', -365);
	    UIActivityUpdates.setCookies(cookieKey, value, 365);
		},
			  
	  init: function (inputs) {
	    console.log('init');
	    //
	    var form = UIActivityUpdates;
	    
	    //
	    form.currentServerTime = inputs.currentServerTime*1 || 0;
	    form.numberOfUpdatedActivities = inputs.numberOfUpdatedActivities*1 || 0;
	    form.cookieName = inputs.cookieName || "";
	    form.currentRemoteId = inputs.currentRemoteId || "";
	    form.noUpdates = inputs.noUpdates || "";
	    form.selectedMode = inputs.selectedTab || "";
	    form.isOnMyActivities = inputs.isOnMyActivities || false;
	    
	    form.currentSelectedTabCookieKey =  form.CURRENT_SELECTED_TAB_KEY.replace(form.REMOTE_ID_PART, form.currentRemoteId);
	  
	    form.clientTimerAtStart = new Date().getTime();
	    
	    //
	    if ( !form.selectedMode ) {
	      form.selectedMode = form.ALL;
	    }
    
      //
      //var lastUpdatedActivitiesNumKey = form.LAST_UPDATED_ACTIVITIES_NUM.replace(form.TAB_PART, form.selectedMode);
	  //  lastUpdatedActivitiesNumKey = lastUpdatedActivitiesNumKey.replace(form.REMOTE_ID_PART, form.currentRemoteId);
	  //  form.resetCookie(lastUpdatedActivitiesNumKey, form.numberOfUpdatedActivities);

      
      //
      $.each($('#UIActivitiesLoader').find('.UIActivity'), function(i, item) {
        if(i < form.numberOfUpdatedActivities) {
          $(item).addClass('UpdatedActivity');
        }
      });
	
	    function isScrolledIntoView() {
     	  console.log('isScrolledIntoView');
	      var docViewTop = $(window).scrollTop();
	      var docViewBottom = docViewTop + $(window).height();
	      var elem = $('#UIActivitiesLoader').find('.UpdatedActivity:last');
	      if(elem.length > 0) {
	        var elemTop = elem.offset().top;
	        var elemBottom = elemTop + $(elem).height();
	        return ((elemBottom >= docViewTop) && (elemTop <= docViewBottom));
	      }
	      return false;
	    }
	    
	    function runOnScroll() {
	      if (isScrolledIntoView()) {
	        form.unMarkedAsUpdate();
	        if (form.numberOfUpdatedActivities > 0) {
              form.resetCookiesOnTabs();
	        }
            
	        $(window).off('scroll', runOnScroll);
	      }
	    }
	
	    $(window).on('scroll', runOnScroll );
	    
	    //
			function checkRefresh()    {
	       var today = new Date();
	       var now = today.getUTCSeconds();
	   
	       var cookie = document.cookie;
	       var cookies = cookie.split('; ');
	   
	       
	       var result = {};
	       for (var i = 0; i < cookies.length; i++) {
	           var cur = cookies[i].split('=');
	           result[cur[0]] = cur[1];
	       }
	       
	       var cookieTime = parseInt( result.SHTS );
	       var cookieName = result.SHTSP;
	   
	       if( cookieName && cookieTime && cookieName == escape(location.href) &&  Math.abs(now - cookieTime) <= 5 ) {
	         // set last Updated Number onto cookie
	         //var lastUpdatedActivitiesNumKey = form.LAST_UPDATED_ACTIVITIES_NUM.replace(form.TAB_PART, form.selectedMode);
			 //    lastUpdatedActivitiesNumKey = lastUpdatedActivitiesNumKey.replace(form.REMOTE_ID_PART, form.currentRemoteId);
			 //    form.resetCookie(lastUpdatedActivitiesNumKey, form.numberOfUpdatedActivities);
			     
		     //  if (form.numberOfUpdatedActivities == 0) {
	         //  form.resetCookiesOnTabs();
			 //    }
	       }   
			 };
			           
       function prepareForRefresh() {
			   if( refresh_prepare > 0 ) {
					 var today = new Date();
					 var now = today.getUTCSeconds();
					 form.setCookies('SHTS', now);
					 form.setCookies('SHTSP', window.location.href);
				 } else {
           form.setCookies('SHTS', '0');
           form.setCookies('SHTSP', ' ');
				 }
       };
			           
			 var refresh_prepare = 1;
			
		   $(window).unload(function(){
		     //prepareForRefresh();
		   }); 
			           
			 $(window).load(function() {
			   checkRefresh();
			 });
	  
	  },
	  unMarkedAsUpdate : function() {
  	    console.log('unMarkedAsUpdate');
	    var form = UIActivityUpdates;
	    var updatedEls = $('#UIActivitiesLoader').find('.UpdatedActivity');
	    
	    updatedEls.removeClass('UpdatedActivity');
	    
	    $('#numberInfo').html(form.noUpdates);
	    
	    
	  },
	  resetCookiesOnTabs : function() {
	    var form = UIActivityUpdates;
	    var userId = UIActivityUpdates.currentRemoteId;
	    var onSelectedTabCookieName = form.CURRENT_SELECTED_TAB_KEY.replace(form.REMOTE_ID_PART, userId);
      var selectedTab = eXo.core.Browser.getCookie(onSelectedTabCookieName);
      if ( selectedTab == null || $.trim(selectedTab).length === 0 ) {
        selectedTab = form.ALL;
        form.resetCookie(onSelectedTabCookieName, form.ALL);
      }
      
      console.log('resetCookiesOnTabs');
      form.applyChanges([selectedTab]);
      
	    // [All Activities] is current Selected tab then reset all other tabs on visited time
	    if ( selectedTab === form.ALL ) {
	      form.applyChanges([form.CONNECTIONS, form.MY_SPACES, form.MY_ACTIVITIES]);
	    }
	  },
	  initCookiesForFirstRun : function(userId, currentServerTime) {
      var form = UIActivityUpdates;
	    form.currentRemoteId = userId;
			var checked_tab_key_from = form.LAST_VISTED_TIME_FROM.replace(form.TAB_PART, form.ALL).replace(form.REMOTE_ID_PART, userId);
			var checked_tab_cookie_from = eXo.core.Browser.getCookie(checked_tab_key_from);
			
			// check if the first run or not, if not then quit
			if ( checked_tab_cookie_from && checked_tab_cookie_from.length > 0 ) {
			  return;
			}
			
			console.log('initCookiesForFirstRun');
			// init timer
			form.clientTimerAtStart = new Date().getTime();
      form.currentServerTime = currentServerTime*1;
        
			form.initValueOnTabs([form.ALL, form.CONNECTIONS, form.MY_SPACES, form.MY_ACTIVITIES]);
			
			//
			var currentSelectedTabCookieKey = form.CURRENT_SELECTED_TAB_KEY.replace(form.REMOTE_ID_PART, userId);
			
			form.resetCookie(currentSelectedTabCookieKey, form.ALL);
	  },
	  removeUpdateInfo : function() {
	    console.log('removeUpdateInfo');
	    var updatedEls = $('#UIActivitiesLoader').find('.UpdatedActivity');
	    var updatedInfoBox = $('#UIActivitiesLoader').find('.UpdateInfo');
      updatedEls.removeClass('UpdatedActivity');
      updatedInfoBox.remove();
	  },
	  setFromCookie : function(from_key, to_key) {
	    console.log('setFromCookie');
	    var to_value = eXo.core.Browser.getCookie(to_key);
	    UIActivityUpdates.resetCookie(from_key, to_value);
	  },
	  setToCookie : function(to_key, value) {
	    console.log('setToCookie');
	    UIActivityUpdates.resetCookie(to_key, value);
	  },
	  initValueOnTabs : function( affectedFields ) { // FIELDS
	    console.log('initValueOnTabs');
	    if ( affectedFields.length === 0 ) return;
	    
	    var form = UIActivityUpdates;
	    var userId = form.currentRemoteId;
	    $.each( affectedFields, function( index, field ) {
			  var checked_tab_key_from = form.LAST_VISTED_TIME_FROM.replace(form.TAB_PART, field).replace(form.REMOTE_ID_PART, userId);
			  var checked_tab_key_old_from = form.LAST_VISTED_TIME_OLD_FROM.replace(form.TAB_PART, field).replace(form.REMOTE_ID_PART, userId);
			  var checked_tab_key_to = form.LAST_VISTED_TIME_TO.replace(form.TAB_PART, field).replace(form.REMOTE_ID_PART, userId);
			  
			  form.setToCookie(checked_tab_key_to, form.calculateServerTime());
			  form.setFromCookie(checked_tab_key_from, checked_tab_key_to);
			  form.setFromCookie(checked_tab_key_old_from, checked_tab_key_from);
			  
			  //			  			
			  var lastUpdatedActivitiesNumKey = form.LAST_UPDATED_ACTIVITIES_NUM.replace(form.TAB_PART, field);
		    lastUpdatedActivitiesNumKey = lastUpdatedActivitiesNumKey.replace(form.REMOTE_ID_PART, userId);
		    form.resetCookie(lastUpdatedActivitiesNumKey, 0);
			});
	  },
	  applyChanges : function( affectedFields ) { // FIELDS
	    if ( affectedFields.length === 0 ) return;
	    console.log('applyChanges');
	    var form = UIActivityUpdates;
	    var userId = form.currentRemoteId;
	    $.each( affectedFields, function( index, field ) {
	      var checked_tab_key_from = form.LAST_VISTED_TIME_FROM.replace(form.TAB_PART, field).replace(form.REMOTE_ID_PART, userId);
	      var checked_tab_key_old_from = form.LAST_VISTED_TIME_OLD_FROM.replace(form.TAB_PART, field).replace(form.REMOTE_ID_PART, userId);
	      var checked_tab_key_to = form.LAST_VISTED_TIME_TO.replace(form.TAB_PART, field).replace(form.REMOTE_ID_PART, userId);
	      
	      form.setFromCookie(checked_tab_key_old_from, checked_tab_key_from);
	      form.setFromCookie(checked_tab_key_from, checked_tab_key_to);
	      form.setToCookie(checked_tab_key_to, form.calculateServerTime());
	    });
	  },
	  calculateServerTime : function() {
	    console.log('calculateServerTime');
	    var form = UIActivityUpdates;
	    var currentClientTime = new Date().getTime();
	    var duration = currentClientTime - form.clientTimerAtStart;
	    var currentServerTime = form.currentServerTime + duration;
	    return $.trim(currentServerTime);
	  }
	};

  return UIActivityUpdates;
})($);