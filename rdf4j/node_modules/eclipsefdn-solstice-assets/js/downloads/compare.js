$(document).ready(function() {

	var default_caption = 'Compare';
	var reset_button = '<input class="button" id="btn-reset" type="reset" value="Reset"/>';

	$('#row-1').show();
	$('#button-control').html('<input id="btn-action" class="btn btn-primary" name="btn-action" type="button" value="' + default_caption + '" />');

	$('#btn-action').click(function() {

		var checkCount = 0;
		$('input:checkbox[name=controls]').each(function () {
		  if ($(this).is(':checked')) {
			  checkCount++;
		  }
		});

		if(checkCount < 2 && $(this).attr('value') == 'Compare'){
			alert('Please choose 2 or more packages for comparison.');

		}else{
			if($(this).attr('value') == 'Compare') {
				$(this).attr('value', 'Show all');
				$('#row-1').hide();
				$('#btn-reset').hide();
				$('input:checkbox[name=controls]').each(function() {
					if(!this.checked){
						var value = $(this).val();
						$('.' + value).hide();
					}
				});
			}else{
				$(this).attr('value', default_caption);
				/*$('#form-compare').trigger("reset");*/
				$('#btn-reset').show();
				$('#compareTable td').each(function() {
					$(this).show();
					$('#row-1').show();
				});
			}
		}
	});
});