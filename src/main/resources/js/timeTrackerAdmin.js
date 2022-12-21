AJS.$(document).ready(function() {
    AJS.$('input.radio').click(function(){
        $("#time_offset").prop( "disabled", !$("#radioButton4").is(':checked') );
    });


    $("#time_offset").prop( "disabled", !$("#radioButton4").is(':checked') );

});
