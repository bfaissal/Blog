angular.module('blog', ['ngSanitize','ngCkeditor','ngTagsInput','ngAnimate'])
    .controller('adminCtrl', ['$scope','$http','$location', function($scope,$http,$location) {
        $scope.posts = [];
        $scope.tinymceOptions = {
            plugins: ["image","code"]
        };
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

        if($location.path().indexOf("/form/") != -1 ){
            $http.get('/postById/'+$location.path().split("/")[2]).success(function(data){
                $scope.post = data;
                $scope.edit = true;
            })

        }
        $scope.$watch(function () {return $location.path()}, function (newLocation, oldLocation) {

                console.info('Why did you use history back?= '+$scope.post);
                if($location.path().indexOf("/form/") != -1 ){
                    console.info($scope.post)
                    if($scope.post == undefined || $scope.title == undefined){
                        $http.get('/postById/'+$location.path().split("/")[2]).success(function(data){
                            $scope.post = data;
                            $scope.edit = true;
                        })
                    }
                    $scope.edit = true;

                }
                else{
                    $scope.edit = false;
                }

        });
        $scope.loadTags = function(query) {
            return $http.get('/tags?query=' + query);
        };
        $scope.orderPridicate="-creationDate";
        $http.get('/allPosts').success(function(data){
            $scope.posts = data;
            $scope.firstPost = $scope.posts[0].creationDate
            console.info(">>> "+$scope.posts.length)
            if($scope.posts.length>0)   {
                console.info($scope.posts[$scope.posts.length-1])
                $scope.lastPost = $scope.posts[$scope.posts.length-1].creationDate
            }
        })
        $scope.getCurrentPagePosts = function (ff){

            var url= '/allPosts?l='+$scope.firstPost
            if(ff)
                var url= '/allPosts?f='+$scope.lastPost


            $http.get(url).success(function(data){
                $scope.posts = data;
                $scope.firstPost = $scope.posts[0].creationDate
                if($scope.posts.length>0)
                    $scope.lastPost = $scope.posts[$scope.posts.length-1].creationDate
            })
        }
        $scope.save = function(){
            $http.post('/savePost',$scope.post).success(function(data){
                if(!$scope.post._id ){
                    $scope.posts.push(data)
                }
                $scope.edit = false;
            })
        }
        $scope.deletePost = function(msg,post){
            if(confirm(msg,post)){
                $http.post('deletePost',post).success(function(){post.deleted = true;})

            }
        }
        $scope.publishPost = function(msg,post,publish){
            if(confirm(msg,post)){
                post.published=publish;
                $http.post('savePost',post);
            }
        }
        $scope.cancelEdit = function(){
            $scope.edit = false;
            $location.path("/form");
        }
        $scope.editPost = function(post){
            $location.path("/form/"+post._id.$oid);
            $scope.edit = true;
            $scope.post = post;
        }
        $scope.addPost = function(){
            $scope.edit = true;
            $scope.post = {};
        }


        $scope.stylefunction = function(i){
            var position = $('#bodyTextArea' ).caret()//getCursorPosition()
            var a = $scope.post.body;
            var style = "left"
            switch(i){
                case 0: style = "left" ;break;
                case 1: style = "center";break;
                case 2: style = "right";break;
            }
            var b = "\n<p style='text-align: "+style+"'>\n\r</p>\n"
            if(i == 3){
                b = "\n<b>\n\r</b>\n"
            }
            if(i == 4){
                b = "\n<i>\n\r</i>\n"
            }
            if(i == 5){
                b = "\n<br/>\n"
            }
            if(i == 6){
                b = "\n<blockquote>\n\r</blockquote>\n"
            }

            $scope.post.body = [a.slice(0, position), b, a.slice(position)].join('');
            $('#bodyTextArea' ).caret(position)
            console.info(position)
            //$('#bodyTextArea').focus();
            console.info("==> "+position)
            $('#bodyTextArea').caret(position)
        }

    }]).config(function($locationProvider) {
        $locationProvider.html5Mode(true).hashPrefix('!');
    });