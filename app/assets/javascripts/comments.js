/**
 * Created by faissalboutaounte on 15-01-27.
 */

angular.module('comment', ['vcRecaptcha'])
    .controller('commentCtrl', ['$scope','$http','vcRecaptchaService','$location', function($scope,$http,recaptcha,$location) {

        $scope.comment = {};
        $scope.comments = [];
        var url = $location.absUrl().replace("post","comments");

        $http.get(url).success(function(data){
            if(data.comments) {
                $scope.comments = data.comments;
            }
            $scope.comment = {};
        });
        $scope.sendComment = function(){
            $scope.comment.recaptcha = recaptcha.getResponse();
            $http.post($location.path(),$scope.comment).success(function(data){
                $scope.comments.push(data);
                $scope.comment = {};
            }).error(function(data){
                alert(data);
            });
        };
        //recaptcha.getResponse()
    }]);
