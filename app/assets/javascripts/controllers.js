angular.module('blog', ['ngSanitize','ngCkeditor','ngTagsInput','ngAnimate'])
    .controller('adminCtrl', ['$scope','$http','$location','$interval', function($scope,$http,$location,$interval) {
        $scope.posts = [];

        $scope.editorOptions = {
            language: 'ar',
            filebrowserUploadUrl: "upload",
            toolbar : [
                { name: 'document', groups: [ 'mode', 'document', 'doctools' ], items: [ 'Source', '-', 'Save', 'NewPage', 'Preview', 'Print', '-', 'Templates' ] },
                { name: 'clipboard', groups: [ 'clipboard', 'undo' ], items: [ 'Cut', 'Copy', 'Paste', 'PasteText', 'PasteFromWord', '-', 'Undo', 'Redo' ] },
                { name: 'editing', groups: [ 'find', 'selection', 'spellchecker' ], items: [ 'Find', 'Replace', '-', 'SelectAll', '-', 'Scayt' ] },
                { name: 'forms', items: [ 'Form', 'Checkbox', 'Radio', 'TextField', 'Textarea', 'Select', 'Button', 'ImageButton', 'HiddenField' ] },
                '/',
                { name: 'basicstyles', groups: [ 'basicstyles', 'cleanup' ], items: [ 'Bold', 'Italic', 'Underline', 'Strike', 'Subscript', 'Superscript', '-', 'RemoveFormat' ] },
                { name: 'paragraph', groups: [ 'list', 'indent', 'blocks', 'align', 'bidi' ], items: [ 'NumberedList', 'BulletedList', '-', 'Outdent', 'Indent', '-', 'Blockquote', 'CreateDiv', '-', 'JustifyLeft', 'JustifyCenter', 'JustifyRight', 'JustifyBlock', '-', 'BidiLtr', 'BidiRtl', 'Language' ] },
                { name: 'links', items: [ 'Link', 'Unlink', 'Anchor' ] },
                { name: 'insert', items: [ 'Image', 'Flash', 'Table', 'HorizontalRule', 'Smiley', 'SpecialChar', 'PageBreak', 'Iframe' ] },
                '/',
                { name: 'styles', items: [ 'Styles', 'Format', 'Font', 'FontSize' ] },
                { name: 'colors', items: [ 'TextColor', 'BGColor' ] },
                { name: 'tools', items: [ 'Maximize', 'ShowBlocks' ] },
                { name: 'others', items: [ '-' ] },
                { name: 'about', items: [ 'About' ] }
            ],


            toolbarGroups: [
                { name: 'document', groups: [ 'mode', 'document', 'doctools' ] },
                { name: 'clipboard', groups: [ 'clipboard', 'undo' ] },
                { name: 'editing', groups: [ 'find', 'selection', 'spellchecker' ] },
                { name: 'forms' },
                '/',
                { name: 'basicstyles', groups: [ 'basicstyles', 'cleanup' ] },
                { name: 'paragraph', groups: [ 'list', 'indent', 'blocks', 'align', 'bidi' ] },
                { name: 'links' },
                { name: 'insert' },
                '/',
                { name: 'styles' },
                { name: 'colors' },
                { name: 'tools' },
                { name: 'others' },
                { name: 'about' }
            ]
        };
        var stop = {};

        if($location.path().indexOf("/form/") != -1 ){
            $http.get('/postById/'+$location.path().split("/")[2]).success(function(data){
                $scope.post = data;
                $scope.edit = true;
            });

        }
        $scope.$watch(function(){return $location.path();}, function (newLocation, oldLocation) {

            if(!$scope.edit){
                if($location.path().indexOf("/form/") != -1 ){

                    if($scope.post === undefined || $scope.title === undefined){
                        $http.get('/postById/'+$location.path().split("/")[2]).success(function(data){
                            $scope.post = data;
                            $scope.edit = true;
                        });
                    }
                    $scope.edit = true;
                    stop = $interval(function() {
                        $http.post('/savePost/true',$scope.post).success(function(data){

                        });
                    },10000);

                }
                else{
                    $scope.edit = false;
                }
            }else{
                if($location.path().indexOf("/form/") == -1 ){
                    $scope.edit = false;
                }
            }

        });

        ARABICM.addImage = function(img){
            $scope.post.imgs = $scope.post.imgs || [];
            try{
                $scope.$apply(function(){
                    $scope.post.imgs.push(img);
                });
            }catch(e){}

        };
        $scope.loadTags = function(query) {
            return $http.get('/tags?query=' + query);
        };
        $scope.newTagAdded = function(tag) {

            $http.post('/tags',tag);
        };

        $scope.orderPridicate="-creationDate";
        $http.get('/allPosts').success(function(data){
            $scope.posts = data;


            if($scope.posts && $scope.posts.length>0)   {
                $scope.firstPost = $scope.posts[0].creationDate;

                $scope.lastPost = $scope.posts[$scope.posts.length-1].creationDate;
            }
        });
        $scope.getCurrentPagePosts = function (ff){

            var url= '/allPosts?l='+$scope.firstPost;
            if(ff) {
                url = '/allPosts?f=' + $scope.lastPost;
            }


            $http.get(url).success(function(data){
                $scope.posts = data;
                $scope.firstPost = $scope.posts[0].creationDate ;
                if($scope.posts.length>0)
                    $scope.lastPost = $scope.posts[$scope.posts.length-1].creationDate ;
            });
        };
        $scope.save = function(){
            $interval.cancel(stop);
            stop = undefined;
            var inputs = [];
            for(var aTag in $scope.post.tags){
                inputs.push($scope.post.tags[aTag].text);
            }
            //$scope.post.url = $scope.post.title
            $http.post('/savePost/false',$scope.post).success(function(data){
                if(!$scope.post._id ){
                    $scope.posts.push(data);
                }
                $scope.edit = false;
                $location.path("/form");
            });
        };
        $scope.deletePost = function(msg,post){
            if(confirm(msg,post)){
                $http.post('deletePost',post).success(function(){post.deleted = true;});

            }
        };
        $scope.publishPost = function(msg,post,publish){
            if(confirm(msg,post)){
                post.published=publish;
                $http.post('/savePost/false',post);
            }
        };
        $scope.cancelEdit = function(){
            $scope.edit = false;
            $location.path("/form");
            $interval.cancel(stop);
            stop = undefined;
        };
        $scope.editPost = function(post){
            $location.path("/form/"+post._id.$oid);
            $scope.edit = true;
            $scope.post = post;
            stop = $interval(function() {
                $http.post('/savePost/true',$scope.post).success(function(data){

                });
            },10000);
        };
        $scope.addPost = function(){
            $scope.edit = true;
            $scope.post = {};
        };




    }]).config(function($locationProvider) {
        $locationProvider.html5Mode(true).hashPrefix('!');
    });