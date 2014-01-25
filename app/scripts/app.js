'use strict';

angular.module('app', [
      'ngCookies',
      'ngResource',
      'ngSanitize',
      'security',
      'services.httpRequestTracker',
      'ui.router'
    ])
    .config(function ($stateProvider, $urlRouterProvider) {
      $urlRouterProvider.otherwise('/');
      $stateProvider
          .state('index', {
            url: '/',
            templateUrl: 'templates/main',
            controller: 'MainCtrl'
          })
          .state('index.login', {
            url: '/login'
          });
//      $routeProvider
//          .when('/', {
//            templateUrl: 'partials/main',
//            controller: 'MainCtrl'
//          })
//          .otherwise({
//            redirectTo: '/'
//          });
//
//      $locationProvider.html5Mode(true);
    })
    .run(['security', function (security) {
      security.requestCurrentUser();
    }])
    .constant('I18N.MESSAGES', {
      'login.reason.notAuthorized': 'You do not have the necessary access permissions.  Do you want to login as someone else?',
      'login.reason.notAuthenticated': 'You must be logged in to access this part of the application.',
      'login.error.invalidCredentials': 'Login failed.  Please check your credentials and try again.',
      'login.error.serverError': 'There was a problem with authenticating: {{exception}}.'
    });