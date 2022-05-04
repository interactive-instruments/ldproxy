const MarkdownIt = require('markdown-it');
const util = require('util');

const md = new MarkdownIt();


function getSideBar(folder, title, parent) {
  const extension = [".md"];
  const dir = parent ? `${parent}/${folder}` : folder;
  const mdPath = path.join(`${__dirname}/../${dir}`);
  const files = fs
    .readdirSync(mdPath)
    .filter(
      (item) =>
        /*item.toLowerCase() != "readme.md" &&*/
        fs.statSync(path.join(mdPath, item)).isFile() &&
        extension.includes(path.extname(item))
    )
    .map(item => {
      getTitle(path.join(mdPath, item));
      return item;
    })
    .map(item => '/' + (dir !== 'en' ? `${dir}/${item}` : item));
  const dirs = fs
  .readdirSync(mdPath)
  .filter(
    (item) => fs.statSync(path.join(mdPath, item)).isDirectory())
  .flatMap(item => getSideBar(item, unslug(item), dir !== 'en' ? dir : null));

  const sidebar = title ? [{ text: title, children: [...files, ...dirs] }] : [...files, ...dirs];


  if (parent === undefined)
    console.log(util.inspect(sidebar, false, null, true));

  return sidebar;
}

function getTitle(mdFile) {
  const data = fs.readFileSync(mdFile, {encoding:'utf8'});
  const first = (data.match(/(^.*)/) || [])[1] || '';
  const tokens = md.parse(first);

  for (var i = 0; i < tokens.length; i++) {
    if (tokens[i].type === 'heading_open') {
      return getRawText(tokens[i + 1].children);
    }
  }
}

function getRawText (tokens) {
  let text = ''

  for (const token of tokens) {
    switch (token.type) {
      case 'text':
      case 'code_inline':
        text += token.content
        break
      case 'softbreak':
      case 'hardbreak':
        text += ' '
        break
    }
  }

  return text
}

function unslug(slug) {
  const arr = slug.split("-");

//loop through each element of the array and capitalize the first letter.


for (var i = 0; i < arr.length; i++) {
    arr[i] = arr[i].charAt(0).toUpperCase() + arr[i].slice(1);

}

//Join all the elements of the array back into a string 
//using a blankspace as a separator 
const str2 = arr.join(" ");
return str2;
}