// Only visible in ProfileDev branch
// local datastore for the App
var data = {
  product: "NextVideo App",
  isActive: isActiveAux,
  seriesList: [1,2,3],
  ProfileID: "NONE"
}

var app = new Vue({
  el:"#app",
  data: data,
  
  methods: {
    GetImage: function (series) {
      //console.log("get image called ");
      imageURL = series.image;
      if (imageURL == null){
        imageURL =  "images/image1.jpg";
      }
      //console.log(imageURL);
      return imageURL;
    },
    PlayNextEpisode: function () {
      series = data.seriesList[currentSelectedCarouselIndex];
      console.log("PlayNextEpisode for " + series.name + " " + series.url);
      //console.log("Season: " + series.currentSeasonNumber);
      //console.log("Episode: " + series.nextEpisodeNumber);
      //console.log("URL: " + series.url);
      toPlayURL = series.url;
      // callJSON(getURL("/Series/") + series.seriesID + "/Play",
      //   // Not sure why but I need to do a $forceUpdate() on the Vue App to get it to refresh properly
      //   function (data2){},
      //   function (error){console.log("Server Error:" + error);},
      // );
      
      this.$root.GotoNextEpisode(null, toPlayURL); // Move on to next episode before we play
      //app.$forceUpdate();
      //window.open(series.url,"_self"); 
    },
    GotoPrevSeason: function (series) {
      series = data.seriesList[currentSelectedCarouselIndex];
      console.log("GotoPrevSeason");
      console.log( "SeriesID: " + series.seriesID );
      callJSON(getURL2("/Series/") + series.seriesID + "/DecSeason",
        // Not sure why but I need to do a $forceUpdate() on the Vue App to get it to refresh properly
        function (data2){data.seriesList[currentSelectedCarouselIndex] = data2; app.$forceUpdate();},
        function (error){console.log("Server Error:" + error);},
      );
    },
  
    GotoPrevEpisode: function () {
      console.log("GotoPrevEpisode");
      series = data.seriesList[currentSelectedCarouselIndex];
      callJSON(getURL2("/Series/") + series.seriesID + "/Dec",
        // Not sure why but I need to do a $forceUpdate() on the Vue App to get it to refresh properly
        function (data2){data.seriesList[currentSelectedCarouselIndex] = data2; app.$forceUpdate();},
        function (error){console.log("Server Error:" + error);},
      );
      
    },
    GotoNextSeason: function () {
      console.log("GotoNextSeason");
      series = data.seriesList[currentSelectedCarouselIndex];
      callJSON(getURL2("/Series/") + series.seriesID + "/IncSeason",
        // Not sure why but I need to do a $forceUpdate() on the Vue App to get it to refresh properly
        function (data2){data.seriesList[currentSelectedCarouselIndex] = data2; app.$forceUpdate();},
        function (error){console.log("Server Error:" + error);},
      )
    },

    showInfo: function () {
        console.log("showInfo");
        series = data.seriesList[currentSelectedCarouselIndex];
        currentSeasonNumber = series.currentSeasonNumber;
        nextEpisodeNumber = series.nextEpisodeNumber;
        urlTemplate = series.infoUrl;
        showURL = urlTemplate.replace("${S}", currentSeasonNumber);
        showURL = showURL.replace("${E}", nextEpisodeNumber);
        // console.log(showURL);
        window.open(showURL,"_self");
    },

    GotoNextEpisode: function (event=null, toPlayURL=null) {
      console.log("GotoNextEpisode");
      // GotoNextEpisode is used by PlayNextEpisode to increment to next and also play
      // Need to get the toPlayURL into the JSON callback function!!!
      // Don't know how to do this neatly so having to use a global - globalToPlayURL
      globalToPlayURL = toPlayURL
      series = data.seriesList[currentSelectedCarouselIndex];
      actionStr = "/Inc";
      if (toPlayURL != null) actionStr = "/Play";
      callJSON(getURL2("/Series/") + series.seriesID + actionStr,
        // Not sure why but I need to do a $forceUpdate() on the Vue App to get it to refresh properly
        function (data2){
          data.seriesList[currentSelectedCarouselIndex] = data2;
          app.$forceUpdate();
          if (globalToPlayURL != null) {
            console.log("About to play: " + globalToPlayURL);
            window.open(globalToPlayURL,"_self");
          }
        },
        function (error){console.log("Server Error:" + error);},
      );
      
    }
  }
})

function HelperMeth() {
  alert("HelperMeth");
}

// Helper function to hide some of the complexity of using fetch and promises
// NOTE: Just using a single errorHandler for 2 promises here (to make it easier)
function callJSON(url, responseHandler, errorHandler) {
  url += getUserParam();
  fetch(url)
      .then(function(response) {
         // Your code for handling the data you get from the API
        response.json().then(responseHandler).catch(errorHandler);
      })
      .catch(errorHandler); 
}

function getURL(relPath){
  //var base1 = "http://18.130.245.71:5000/"; //AWS Server
  // ec2-3-140-251-38.us-east-2.compute.amazonaws.com
  var base1 = "http://ec2-54-217-28-69.eu-west-1.compute.amazonaws.com:5000"
  var base2 = "http://127.0.0.1:5000";
  var base = base1;
  return base + relPath;
}

// getURL2 returns URL for new clojure webserver
function getURL2(relPath){
  return relPath;
}

function getUserParam(){
  return "?UID=" + data.ProfileID;
}

function isActiveAux (index) { 
  return (index == 0 ? "carousel-item active" : "carousel-item") 
}

function getSeriesList(){
  console.log("getSeriesList called");
  //getSeriesListMockUp();
  getSeriesListReal()
}

function getSeriesListMockUp(){
  console.log("getSeriesListMockUp called");
  data.seriesList = [{"seriesID": "GOT", "currentSeasonNumber": 1, "nextEpisodeNumber": 9, "lastWatchedDate": "2020-11-14", "name": "Game of Thrones", "image": null, "url": "http://tvhome.cc/play.php?tid=yweywzy|nktlyeykvy"}, {"seriesID": "Billions", "currentSeasonNumber": 1, "nextEpisodeNumber": "1", "lastWatchedDate": "2018-11-14", "name": "Billions", "image": null}, {"seriesID": "BigBang", "name": "The Big Bang Theory", "image": null, "currentSeasonNumber": 1, "nextEpisodeNumber": 1, "lastWatchedDate": "1900-01-01"}, {"seriesID": "WestWorld", "name": "WestWorld", "image": null, "currentSeasonNumber": 1, "nextEpisodeNumber": 1, "lastWatchedDate": "1900-01-01"}];
}

function getSeriesListReal(){
  callJSON(getURL2("/WatchList"),
        function (data2){data.seriesList = data2;
          console.log(JSON.stringify(data2[0]));
        },
        function (error){console.log("Server Error:" + error);},
      );
}

var currentSelectedCarouselIndex = 0;
var globalToPlayURL = null;

function init(){
  console.log("init called");
  getSeriesList();
 
  $('.carousel').on('slide.bs.carousel',function(e){
    var slideFrom = $(this).find('.active').index();
    currentSelectedCarouselIndex = $(e.relatedTarget).index();
    $(this).carousel("pause"); // prevent the carousel from auto sliding
    //console.log(slideFrom+' => '+currentSelectedCarouselIndex);
  });

  // Getting a strange behaviour with back button on Amazon Fire TV Stick browsers
  // Both Silk and Firefox behave the same and are returning to the original page rather than the updated page
  // Lots of debris code below where I was hunting for an event that gets triggered on the Amazon Fire Stick browsers (I couldn't find one)
  // Settled for the setInterval kludge in the end (see below)
  $(window).focus(function() {
    //DEBUG("focus - welcome (back)");
    console.log('welcome (back)');
    //app.$forceUpdate();
  });

  window.onpageshow = function(event) {
    //DEBUG("onpageshow");
    series = data.seriesList[currentSelectedCarouselIndex];
    console.log('onpageshow' + series);
    
    if (series == null){
      console.log('onpageshow - NULL series');
      refreshApp();
    }
    if (event.persisted) {
      console.log('onpageshow - persisted xx');
      //app.$forceUpdate();
      //refreshApp(); 
    }
  };

  window.onload = function(event) {
    //DEBUG("onload");
  };

  window.onunload = function(event) {
    //DEBUG("onunload");
    console.log('onunload');
  };

  $("#topLeft").click(function() {
    refreshApp();
    saveToLocalStorage("PROFILE_ID");
  });

  $(".profilePerson").click(function() {
    console.log("Person Selected")
    var person = $(this).text();
    console.log(person);
    saveToLocalStorage("PROFILE_ID", person);
    data.ProfileID = person;
    // r=1 tells index.html this is a redirect and prevents it forcing a profile select again
    window.open("index.html?r=1","_self");
  });

  history.navigationMode = 'compatible';
  $(document).ready(function(){
    //DEBUG("READY OR NOT. HERE I COME - Going Find you...");
  })

  
  
}


jQuery(document).ready(function($){
  DEBUG("JQuery Ready called");

  $(window).on('popstate',function(){
    //DEBUG("POPSTATE Force Reload");
    console.log('POPSTATE Force Reload');
    location.reload(true);
  });

  //console.log("Testing Local storage");
  urlPath = String(window.location);
  //console.log(urlPath);
  // Next few lines determine whether this is the first time the App has been called
  // When so force user to select a profile to use.
  urlPath = urlPath.substr(urlPath.length - 10);
  if (urlPath.indexOf(".html") == -1){
    firstTime = true;
  }
  else {
    firstTime = (urlPath == "index.html");
  }
  
  data.ProfileID = getFromLocalStorage("PROFILE_ID")
  if ((data.ProfileID == null)||firstTime) {
    // Set a Defauly profile else will loop
    if (data.ProfileID == null) {
      saveToLocalStorage("PROFILE_ID", "Guest");
    }
    
    // Jump to profile page
    window.open("selectProfile.html","_self");
  }
  console.log(data.ProfileID);

  urlPath = String(window.location);
  if (urlPath.indexOf("selectProfile.html") != -1){
    console.log("Setting focus on Sharon");
    $("#sharonProfile").focus();
  }
  
})

function saveToLocalStorage(key, dataObj) {
  if (dataObj == null){
    localStorage.removeItem(key);
  }
  else {
    localStorage.setItem(key, dataObj);
  }
  
}

function getFromLocalStorage(key) {
  return localStorage.getItem(key);
}


// Kludge to fix a problem on browsers whose back button take you to a cached original state of the page
// If the page is a SPA/Ajax page and was changed then these changes will not be reflected.
// To make the problem worse: I cannot find any events that are triggered after back button is pressed
//
// The kludge sets up a repeating timer that gets re-run in the situation above 
//
var intCount = 0;
var INTERVAL_TIMER = window.setInterval(function(){
  //DEBUG("Interval called: " + intCount++); 
  console.log("refreshApp called xx " + data.ProfileID);
  window.setTimeout(refreshApp, 1000);     // Watch out for this value (and corresponding one below). May be too quick for some browsers! Didin't work on MacOS desktop machine.
  window.clearInterval(INTERVAL_TIMER);
}, 100);  

function DEBUG(msg){
  debugConsole = $("#debugConsole");
  curInner = debugConsole.html(); 
  console.log("DEBUG called - Current InnerText:");
  console.log(curInner);
  curInner += "<br/>" + msg;
  var res = curInner.split("<br>");
  if (res.length > 100){
    console.log("Clearing debug Console");
    curInner = msg;
  }
  debugConsole.html(curInner);
}

function refreshApp(){
  //DEBUG("Refresh App");
  
  init();
  app.$forceUpdate();

  //window.location.replace("http://localhost:8000/index.html?r=1");

}

init();

