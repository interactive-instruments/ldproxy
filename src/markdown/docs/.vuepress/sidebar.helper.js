const { fs, path } = require('@vuepress/utils');
//const util = require('util');

module.exports = {
	createGroup: createGroup,
}

function createGroup(title, directory, options = { children: [], ignoreReadme: false, headerReadme: false, collapsible: false}) {
  const extension = [".md"];
  const dir = normalize(directory);
  const mdPath = path.join(`${__dirname}/..${dir}`);
  const opts = {
    children: [],
    ...options,
  }

  const files = fs
    .readdirSync(mdPath)
    .filter(
      (item) =>
        fs.statSync(path.join(mdPath, item)).isFile() &&
        extension.includes(path.extname(item)) &&
        ((!opts.ignoreReadme && !opts.headerReadme) || item.toLowerCase() !== "readme.md")
    )
    .sort(sortMdFiles)
    .map(item => `${dir}${item}`);
  

  const sidebar = title ? { text: title, children: [...files, ...opts.children], collapsible: opts.collapsible, link: opts.headerReadme ? dir : undefined } : [...files];

  //console.log(util.inspect(sidebar, false, null, true));

  return sidebar;
}

function sortMdFiles(a, b) 
{
  if (a.toLowerCase() === "readme.md") {
    return -1;
  }
  if (a < b)
  {
    return -1;
  }

  if (a > b)
  {
    return 1;
  }

  return 0; 
}

function normalize(directory) {
  let dir = directory ? directory.trim() : '';
  if (dir.charAt(0) !== '/') {
    dir = `/${dir}`;
  }
  if (dir.charAt(dir.length-1) !== '/') {
    dir = `${dir}/`;
  }
  return dir;
}