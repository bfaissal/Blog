/**
 * Created by faissalboutaounte on 15-01-27.
 */

angular.module('comment', ['vcRecaptcha'])
    .controller('commentCtrl', ['$scope','$http','vcRecaptchaService','$location', function($scope,$http,recaptcha,$location) {
        console.info(recaptcha)
        $scope.comment = {}
        $scope.comments = []
        $scope.sendComment = function(){
            console.info(recaptcha.getResponse())
            $scope.comment.recaptcha = recaptcha.getResponse()
            $http.post($location.path(),$scope.comment).success(function(data){
                $scope.comments.push($scope.comment);
                $scope.comment = {}
            }).error(function(data){
                alert(data);
            })
        }
        //recaptcha.getResponse()
    }])
